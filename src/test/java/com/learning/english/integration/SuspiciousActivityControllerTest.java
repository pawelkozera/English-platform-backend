package com.learning.english.integration;

import com.learning.english.dto.SuspiciousActivityAddRequest;
import com.learning.english.models.TestInstance;
import com.learning.english.models.TestTemplate;
import com.learning.english.service.JwtService;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.english.models.*;
import com.learning.english.repository.*;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class SuspiciousActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestHistoryRepository testHistoryRepository;

    @Autowired
    private SuspiciousActivityRepository suspiciousActivityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private TestInstanceRepository testInstanceRepository;

    @Autowired
    private TestTemplateRepository testTemplateRepository;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private Group testGroup;
    private TestTemplate testTemplate;
    private TestHistory testHistory;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testGroup = createTestGroup();
        testTemplate = createTestTemplate();
        testHistory = createTestHistoryForUser(testUser);
    }

    @AfterEach
    void tearDown() {
        suspiciousActivityRepository.deleteAll();
        testHistoryRepository.deleteAll();
        testInstanceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldAddSuspiciousActivitySuccessfully() throws Exception {
        SuspiciousActivityAddRequest addRequest = new SuspiciousActivityAddRequest();
        addRequest.setTestInstanceId(testHistory.getTestInstance().getId());
        addRequest.setDescription("Switching tabs");

        mockMvc.perform(post("/api/v1/suspicious/activity/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Suspicious activity recorded successfully"));

        List<SuspiciousActivity> activities = suspiciousActivityRepository.findAll();
        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).getDescription()).isEqualTo("Switching tabs");
        assertThat(activities.get(0).getOccurrenceCount()).isEqualTo(1);

        TestHistory updatedTestHistory = testHistoryRepository.findById(testHistory.getId()).orElseThrow();
        assertThat(updatedTestHistory.isSuspiciousActivityDetected()).isTrue();
    }

    @Test
    void shouldIncrementOccurrenceCountForExistingSuspiciousActivity() throws Exception {
        SuspiciousActivity existingActivity = createSuspiciousActivity(testHistory, "Switching tabs");

        SuspiciousActivityAddRequest addRequest = new SuspiciousActivityAddRequest();
        addRequest.setTestInstanceId(testHistory.getTestInstance().getId());
        addRequest.setDescription("Switching tabs");

        mockMvc.perform(post("/api/v1/suspicious/activity/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Suspicious activity recorded successfully"));

        SuspiciousActivity updatedActivity = suspiciousActivityRepository.findById(existingActivity.getId()).orElseThrow();
        assertThat(updatedActivity.getOccurrenceCount()).isEqualTo(2);
    }

    @Test
    void shouldReturnErrorWhenTestHistoryNotFound() throws Exception {
        SuspiciousActivityAddRequest addRequest = new SuspiciousActivityAddRequest();
        addRequest.setTestInstanceId(999);
        addRequest.setDescription("Nonexistent test history");

        mockMvc.perform(post("/api/v1/suspicious/activity/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No TestHistory found for the given TestInstance and User"));
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("testuser@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("password123");
        user.setRole(Role.USER);
        jwtToken = jwtService.generateToken(user);
        return userRepository.save(user);
    }

    private Group createTestGroup() {
        Group group = new Group();
        group.setGroupName("Test Group");
        return groupRepository.save(group);
    }

    private TestTemplate createTestTemplate() {
        TestTemplate template = new TestTemplate();
        template.setName("Test Template");
        template.setOwner(testUser);
        return testTemplateRepository.save(template);
    }

    private TestHistory createTestHistoryForUser(User user) {
        TestInstance testInstance = new TestInstance();
        testInstance.setActivationTime(LocalDateTime.now());
        testInstance.setEndTime(LocalDateTime.now().plusHours(1));
        testInstance.setGroup(testGroup);
        testInstance.setTestTemplate(testTemplate);
        testInstance.setUuid(UUID.randomUUID());
        testInstance = testInstanceRepository.save(testInstance);

        TestHistory testHistory = new TestHistory();
        testHistory.setUser(user);
        testHistory.setTestInstance(testInstance);
        testHistory.setSuspiciousActivityDetected(false);
        return testHistoryRepository.save(testHistory);
    }

    private SuspiciousActivity createSuspiciousActivity(TestHistory testHistory, String description) {
        SuspiciousActivity activity = new SuspiciousActivity();
        activity.setTestHistory(testHistory);
        activity.setDescription(description);
        activity.setOccurrenceCount(1);
        activity.setTimestamp(LocalDateTime.now());
        return suspiciousActivityRepository.save(activity);
    }
}