package com.learning.english.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.english.dto.TemplateAddRequest;
import com.learning.english.models.*;
import com.learning.english.repository.*;
import com.learning.english.service.JwtService;
import com.learning.english.service.TestTemplateService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TestTemplateControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskTypeRepository taskTypeRepository;

    @Autowired
    private TaskSubTypeRepository taskSubTypeRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private TestTemplateRepository testTemplateRepository;

    @Autowired
    private TestInstanceRepository testInstanceRepository;

    @Autowired
    private JwtService jwtService;

    private String jwtToken;
    private int groupId;
    private int groupIdWithoutTests;
    private int userId;
    private int taskId;
    private TaskType testTaskType;
    private TaskSubType testTaskSubType;
    private Lesson testLesson;
    private List<Word> testWords;
    private Task testTask;
    private int testTemplateId;

    @BeforeEach
    public void setUp() throws Exception {
        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("testuser@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(user);
        jwtToken = jwtService.generateToken(user);

        Group group = Group.builder()
                .groupName("Test Group")
                .password("password123")
                .groupCode("uniqueCode123")
                .build();

        List<Group> groups = List.of(group);
        groupRepository.saveAll(groups);

        groupId = group.getId();

        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .isOwner(true)
                .build();

        List<UserGroup> userGroups = List.of(userGroup);

        userGroupRepository.saveAll(userGroups);

        userId = user.getId();

        testTaskType = taskTypeRepository.save(
                TaskType.builder()
                        .typeName("typing")
                        .build()
        );

        testTaskSubType = taskSubTypeRepository.save(
                TaskSubType.builder()
                        .subTypeName("translation")
                        .build()
        );

        testLesson = lessonRepository.save(
                Lesson.builder()
                        .title("Test Lesson")
                        .owner(user)
                        .groups(List.of(group))
                        .build()
        );

        testWords = new ArrayList<>(List.of(
                Word.builder().word("cat").translation("kot").createdBy(user).build(),
                Word.builder().word("dog").translation("pies").createdBy(user).build()
        ));

        wordRepository.saveAll(testWords);

        testTask = taskRepository.save(
                Task.builder()
                        .taskType(testTaskType)
                        .taskSubType(testTaskSubType)
                        .lesson(testLesson)
                        .words(testWords)
                        .owner(user)
                        .score(1)
                        .content("")
                        .correctAnswer("")
                        .content("Translate the word")
                        .correctAnswer("kot")
                        .build()
        );

        taskId = testTask.getId();

        List<TestTemplate> testTemplates = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TestTemplate testTemplate = TestTemplate.builder()
                    .name("Test Template " + i)
                    .owner(user)
                    .build();
            testTemplates.add(testTemplate);
        }

        testTemplates = (List<TestTemplate>) testTemplateRepository.saveAll(testTemplates);
        testTemplateId = testTemplates.getFirst().getId();
    }

    @AfterEach
    public void tearDown() {
        testInstanceRepository.deleteAll();
        testTemplateRepository.deleteAll();
        taskRepository.deleteAll();
        wordRepository.deleteAll();
        lessonRepository.deleteAll();
        userGroupRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
        taskTypeRepository.deleteAll();
        taskSubTypeRepository.deleteAll();
    }

    @Test
    void shouldCreateTemplate() throws Exception {
        TemplateAddRequest templateAddRequest = new TemplateAddRequest();
        templateAddRequest.setName("Test Template New");
        templateAddRequest.setTasksIds(List.of(taskId));

        mockMvc.perform(post("/api/v1/test/template/add")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(templateAddRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Template created successfully"));
    }

    @Test
    void shouldReturnForbiddenWhenUserDoesNotOwnTask() throws Exception {
        User newUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test2@example.com")
                .password("password")
                .role(Role.USER)
                .build();
        newUser = userRepository.save(newUser);

        String newJwt = jwtService.generateToken(newUser);

        TemplateAddRequest templateAddRequest = new TemplateAddRequest();
        templateAddRequest.setName("Test Template");
        templateAddRequest.setTasksIds(List.of(taskId));

        mockMvc.perform(post("/api/v1/test/template/add")
                        .cookie(new Cookie("accessToken", newJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(templateAddRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Some tasks do not belong to the user"));
    }

    @Test
    void shouldReturnEmptyPageWhenNoTemplates() throws Exception {
        User anotherUser = User.builder()
                .firstName("Another")
                .lastName("User")
                .email("anotheruser@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(anotherUser);
        String anotherJwtToken = jwtService.generateToken(anotherUser);

        mockMvc.perform(get("/api/v1/test/template/all/owned/by/user")
                        .cookie(new Cookie("accessToken", anotherJwtToken))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void shouldReturnCorrectPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/test/template/all/owned/by/user")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    @Test
    void shouldReturnValidTemplateResponseStructure() throws Exception {
        mockMvc.perform(get("/api/v1/test/template/all/owned/by/user")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.testTemplateResponseList[0].id").isNumber())
                .andExpect(jsonPath("$._embedded.testTemplateResponseList[0].name").value("Test Template 0"))
                .andExpect(jsonPath("$.page.totalElements").value(5));
    }

    @Test
    void shouldReturnUnauthorizedForNoAuthToken() throws Exception {
        mockMvc.perform(get("/api/v1/test/template/all/owned/by/user")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnBadRequestForInvalidPageParam() throws Exception {
        mockMvc.perform(get("/api/v1/test/template/all/owned/by/user")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteTestTemplateByOwner() throws Exception {
        mockMvc.perform(delete("/api/v1/test/template/{testTemplateId}/delete", testTemplateId)
                        .cookie(new Cookie("accessToken", jwtToken)))  // Use the owner's JWT token
                .andExpect(status().isOk())
                .andExpect(content().string("Test instance deleted successfully"));

        Optional<TestTemplate> deletedTemplate = testTemplateRepository.findById(testTemplateId);
        assertThat(deletedTemplate).isEmpty();
    }

    @Test
    void shouldReturnUnauthorizedWhenNonOwnerDeletesTemplate() throws Exception {
        User newUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test2@example.com")
                .password("password")
                .role(Role.USER)
                .build();
        newUser = userRepository.save(newUser);

        String newJwt = jwtService.generateToken(newUser);

        mockMvc.perform(delete("/api/v1/test/template/{testTemplateId}/delete", testTemplateId)
                        .cookie(new Cookie("accessToken", newJwt)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You are not authorized to delete this test template"));
    }

    @Test
    void shouldReturnNotFoundWhenTestTemplateDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/v1/test/template/{testTemplateId}/delete", 9999)
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Test template not found"));
    }

    @Test
    void shouldReturnBadRequestForInvalidTestTemplateId() throws Exception {
        mockMvc.perform(delete("/api/v1/test/template/{testTemplateId}/delete", -1)
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest());
    }
}
