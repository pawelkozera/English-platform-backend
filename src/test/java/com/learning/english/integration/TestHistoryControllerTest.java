package com.learning.english.integration;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.english.dto.TestHistoryAddRequest;
import com.learning.english.models.*;
import com.learning.english.models.TestInstance;
import com.learning.english.models.TestTemplate;
import com.learning.english.repository.*;
import com.learning.english.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class TestHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHistoryRepository testHistoryRepository;

    @Autowired
    private TestInstanceRepository testInstanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private TaskTypeRepository taskTypeRepository;

    @Autowired
    private TaskSubTypeRepository taskSubTypeRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestTemplateRepository testTemplateRepository;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private Group testGroup;
    private TestInstance testInstance;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(
                User.builder()
                        .email("testuser@example.com")
                        .firstName("Test")
                        .lastName("User")
                        .password("password")
                        .role(Role.USER)
                        .build()
        );

        testGroup = Group.builder()
                .groupName("Test Group")
                .password("validPassword")
                .groupCode("validGroupCode")
                .build();

        groupRepository.save(testGroup);

        userGroupRepository.save(
                UserGroup.builder()
                        .user(testUser)
                        .group(testGroup)
                        .isOwner(true)
                        .build()
        );

        TaskType testTaskType = taskTypeRepository.save(
                TaskType.builder()
                        .typeName("typing")
                        .build()
        );

        TaskSubType testTaskSubType = taskSubTypeRepository.save(
                TaskSubType.builder()
                        .subTypeName("translation")
                        .build()
        );

        Lesson testLesson = lessonRepository.save(
                Lesson.builder()
                        .title("Test Lesson")
                        .owner(testUser)
                        .groups(List.of(testGroup))
                        .build()
        );

        List<Word> testWords = new ArrayList<>(List.of(
                Word.builder().word("cat").translation("kot").createdBy(testUser).build(),
                Word.builder().word("dog").translation("pies").createdBy(testUser).build()
        ));

        wordRepository.saveAll(testWords);

        taskRepository.save(
                Task.builder()
                        .taskType(testTaskType)
                        .taskSubType(testTaskSubType)
                        .lesson(testLesson)
                        .words(testWords)
                        .owner(testUser)
                        .content("Translate the word")
                        .correctAnswer("kot")
                        .build()
        );

        TestTemplate testTemplate = testTemplateRepository.save(
                TestTemplate.builder()
                        .name("Test Template")
                        .owner(testUser)
                        .build()
        );

        testInstance = testInstanceRepository.save(
                TestInstance.builder()
                        .group(testGroup)
                        .uuid(UUID.randomUUID())
                        .testTemplate(testTemplate)
                        .activationTime(LocalDateTime.now().minusDays(1))
                        .endTime(LocalDateTime.now().plusDays(1))
                        .build()
        );

        jwtToken = jwtService.generateToken(testUser);
    }

    @AfterEach
    void tearDown() {
        testHistoryRepository.deleteAll();
        testInstanceRepository.deleteAll();
        testTemplateRepository.deleteAll();
        taskRepository.deleteAll();
        wordRepository.deleteAll();
        taskTypeRepository.deleteAll();
        taskSubTypeRepository.deleteAll();
        lessonRepository.deleteAll();
        userGroupRepository.deleteAll();
        userRepository.deleteAll();
        groupRepository.deleteAll();
    }

    @Test
    void shouldReturnBadRequestWhenTestInstanceNotFound() throws Exception {
        TestHistoryAddRequest request = new TestHistoryAddRequest();
        request.setTestInstanceId(999);
        request.setScore(80);

        mockMvc.perform(post("/api/v1/test/history/add")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Test instance not found"));
    }

    @Test
    void shouldReturnForbiddenWhenTestInstanceHasAlreadyEnded() throws Exception {
        testInstance.setEndTime(LocalDateTime.now().minusHours(1));
        testInstanceRepository.save(testInstance);

        TestHistoryAddRequest request = new TestHistoryAddRequest();
        request.setTestInstanceId(testInstance.getId());
        request.setScore(80);

        mockMvc.perform(post("/api/v1/test/history/add")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Access denied: Test has already ended or hasn't started yet"));
    }

    @Test
    void shouldReturnForbiddenWhenTestAlreadyCompleted() throws Exception {
        TestHistory existingTestHistory = new TestHistory();
        existingTestHistory.setTestInstance(testInstance);
        existingTestHistory.setUser(testUser);
        existingTestHistory.setScore(100);
        existingTestHistory.setCompletedAt(LocalDateTime.now().minusMinutes(10));
        testHistoryRepository.save(existingTestHistory);

        TestHistoryAddRequest request = new TestHistoryAddRequest();
        request.setTestInstanceId(testInstance.getId());
        request.setScore(80);

        mockMvc.perform(post("/api/v1/test/history/add")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Test already completed"));
    }

    @Test
    void shouldSaveTestProgressSuccessfully() throws Exception {
        TestHistoryAddRequest request = new TestHistoryAddRequest();
        request.setTestInstanceId(testInstance.getId());
        request.setScore(4);

        mockMvc.perform(post("/api/v1/test/history/add")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Test started successfully"));

        Optional<TestHistory> afterTestHistoryOpt = testHistoryRepository.findByTestInstanceAndUser(testInstance, testUser);
        assertTrue(afterTestHistoryOpt.isPresent());
    }

    @Test
    void shouldStartTestSuccessfullyWhenNoExistingTestHistory() throws Exception {
        testInstance.setActivationTime(LocalDateTime.now().minusDays(1));
        testInstance.setEndTime(LocalDateTime.now().plusDays(1));
        testInstanceRepository.save(testInstance);

        TestHistoryAddRequest request = new TestHistoryAddRequest();
        request.setTestInstanceId(testInstance.getId());
        request.setScore(-1);

        mockMvc.perform(post("/api/v1/test/history/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Test started successfully"));

        Optional<TestHistory> testHistoryOpt = testHistoryRepository.findByTestInstanceAndUser(testInstance, testUser);
        assertTrue(testHistoryOpt.isPresent());
        assertEquals(testHistoryOpt.get().getScore(), 0);
    }

    @Test
    void shouldReturnNotFoundWhenTestInstanceDoesNotExistForCheckCompletion() throws Exception {
        mockMvc.perform(get("/api/v1/test/history/check/completion/999")
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Test instance not found"));
    }

    @Test
    void shouldReturnNotFoundWhenTestHistoryDoesNotExistForUser() throws Exception {
        testHistoryRepository.deleteAll();

        mockMvc.perform(get("/api/v1/test/history/check/completion/" + testInstance.getId())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Test history not found for user"));
    }

    @Test
    void shouldReturnForbiddenWhenTestHasAlreadyEnded() throws Exception {
        TestHistory testHistory = TestHistory.builder()
                .testInstance(testInstance)
                .user(testUser)
                .completedAt(LocalDateTime.now().minusDays(1))
                .build();
        testHistoryRepository.save(testHistory);

        mockMvc.perform(get("/api/v1/test/history/check/completion/" + testInstance.getId())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnOkWhenTestIsStillActive() throws Exception {
        TestHistory testHistory = TestHistory.builder()
                .testInstance(testInstance)
                .user(testUser)
                .completedAt(null)
                .build();
        testHistoryRepository.save(testHistory);

        mockMvc.perform(get("/api/v1/test/history/check/completion/" + testInstance.getId())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Test is still active"));
    }

    @Test
    void shouldReturnTestHistoryForUserInGroup() throws Exception {
        TestHistory testHistory = TestHistory.builder()
                .testInstance(testInstance)
                .user(testUser)
                .score(85)
                .build();

        testHistoryRepository.save(testHistory);

        mockMvc.perform(get("/api/v1/test/history/all/for/display/" + testGroup.getId())
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.testHistoryDisplayResponseList[0].testName").value("Test Template"))
                .andExpect(jsonPath("$._embedded.testHistoryDisplayResponseList[0].score").value(85));
    }

    @Test
    void shouldReturn403WhenUserNotInGroup() throws Exception {
        User testUser2 = userRepository.save(
                User.builder()
                        .email("testuser23@example.com")
                        .firstName("Test")
                        .lastName("User")
                        .password("password")
                        .role(Role.USER)
                        .build()
        );

        String jwt = jwtService.generateToken(testUser2);

        mockMvc.perform(get("/api/v1/test/history/all/for/display/" + testGroup.getId())
                        .cookie(new Cookie("accessToken", jwt))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnEmptyListWhenNoTestHistoryExists() throws Exception {
        testHistoryRepository.deleteAll();

        mockMvc.perform(get("/api/v1/test/history/all/for/display/" + testGroup.getId())
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }
}