package com.learning.english.integration;

import com.learning.english.models.*;
import com.learning.english.repository.*;
import com.learning.english.service.JwtService;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TestInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestTemplateRepository testTemplateRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestInstanceRepository testInstanceRepository;

    @Autowired
    private AuthUtil authUtil;

    private String jwtToken;
    private int groupId;
    private int groupIdWithoutTests;
    private int testTemplateId;
    private int userId;

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

        Group groupWithoutTests = Group.builder()
                .groupName("Test Group without tests")
                .password("password123")
                .groupCode("uniqueCode1234")
                .build();

        List<Group> groups = List.of(group, groupWithoutTests);
        groupRepository.saveAll(groups);

        groupId = group.getId();
        groupIdWithoutTests = groupWithoutTests.getId();

        TestTemplate testTemplate = TestTemplate.builder()
                .name("Test Template")
                .owner(user)
                .build();
        testTemplateRepository.save(testTemplate);
        testTemplateId = testTemplate.getId();

        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .isOwner(true)
                .build();

        UserGroup userGroupWithoutTests = UserGroup.builder()
                .user(user)
                .group(groupWithoutTests)
                .isOwner(true)
                .build();

        List<UserGroup> userGroups = List.of(userGroup, userGroupWithoutTests);

        userGroupRepository.saveAll(userGroups);

        userId = user.getId();

        testInstanceRepository.save(
                TestInstance.builder()
                        .group(group)
                        .uuid(UUID.randomUUID())
                        .testTemplate(testTemplate)
                        .activationTime(LocalDateTime.now().minusDays(1))
                        .endTime(LocalDateTime.now().plusDays(1))
                        .build()
        );
    }

    @AfterEach
    public void tearDown() {
        testInstanceRepository.deleteAll();
        testTemplateRepository.deleteAll();
        userGroupRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testLaunchTestInstanceAsOwner() throws Exception {
        String testInstanceJson = "{\"testTemplateId\": " + testTemplateId + ", \"groupId\": " + groupId + ", \"activationTime\": \"2024-12-25T12:00:00\", \"endTime\": \"2024-12-25T14:00:00\", \"timeDuration\": 120}";

        mockMvc.perform(post("/api/v1/test/instance/add")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(testInstanceJson))
                .andExpect(status().isOk());

        assertTrue(testInstanceRepository.existsByTestTemplateIdAndGroupId(testTemplateId, groupId));
    }

    @Test
    public void testLaunchTestInstanceAsNonOwner() throws Exception {
        User nonOwner = User.builder()
                .firstName("NonOwner")
                .lastName("User")
                .email("nonowner@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(nonOwner);

        String nonOwnerToken = jwtService.generateToken(nonOwner);

        String testInstanceJson = "{\"testTemplateId\": " + testTemplateId + ", \"groupId\": " + groupId + ", \"activationTime\": \"2024-12-25T12:00:00\", \"endTime\": \"2024-12-25T14:00:00\", \"timeDuration\": 120}";

        mockMvc.perform(post("/api/v1/test/instance/add")
                        .cookie(new Cookie("accessToken", nonOwnerToken))
                        .contentType("application/json")
                        .content(testInstanceJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLaunchTestInstanceWithoutValidTestTemplate() throws Exception {
        String invalidTestTemplateId = "999999";
        String testInstanceJson = "{\"testTemplateId\": " + invalidTestTemplateId + ", \"groupId\": " + groupId + ", \"activationTime\": \"2024-12-25T12:00:00\", \"endTime\": \"2024-12-25T14:00:00\", \"timeDuration\": 120}";

        mockMvc.perform(post("/api/v1/test/instance/add")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(testInstanceJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLaunchTestInstanceWithoutGroupOwnerPermission() throws Exception {
        User nonOwner = User.builder()
                .firstName("NonOwner")
                .lastName("User")
                .email("nonowner@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(nonOwner);

        String nonOwnerToken = jwtService.generateToken(nonOwner);

        String testInstanceJson = "{\"testTemplateId\": " + testTemplateId + ", \"groupId\": " + groupId + ", \"activationTime\": \"2024-12-25T12:00:00\", \"endTime\": \"2024-12-25T14:00:00\", \"timeDuration\": 120}";

        mockMvc.perform(post("/api/v1/test/instance/add")
                        .cookie(new Cookie("accessToken", nonOwnerToken))
                        .contentType("application/json")
                        .content(testInstanceJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetTestInstancesForDisplay_Success() throws Exception {
        mockMvc.perform(get("/api/v1/test/instance/all/for/display/{groupId}", groupId)
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.testInstanceDisplayResponseList[0].testName").value("Test Template"));
    }

    @Test
    public void testGetTestInstancesForDisplay_AccessDenied() throws Exception {
        User nonOwner = User.builder()
                .firstName("NonOwner")
                .lastName("User")
                .email("nonowner@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(nonOwner);

        String nonOwnerToken = jwtService.generateToken(nonOwner);

        mockMvc.perform(get("/api/v1/test/instance/all/for/display/{groupId}", groupId)
                        .cookie(new Cookie("accessToken", nonOwnerToken))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetTestInstancesForDisplay_NoTestInstancesFound() throws Exception {
        mockMvc.perform(get("/api/v1/test/instance/all/for/display/{groupId}", groupIdWithoutTests)
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }
}