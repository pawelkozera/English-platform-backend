package com.learning.english.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.english.dto.GroupAddLessonsRequest;
import com.learning.english.dto.GroupJoinRequest;
import com.learning.english.models.*;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.LessonRepository;
import com.learning.english.repository.UserGroupRepository;
import com.learning.english.repository.UserRepository;
import com.learning.english.service.JwtService;
import com.learning.english.utils.AuthUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private AuthUtil authUtil;

    private String jwtToken;
    private Group testGroup;
    private Group testGroupNotJoined;
    private Group testGroupNotOwner;
    private Lesson testLesson1;
    private Lesson testLesson2;

    @BeforeEach
    public void setUp() throws Exception {
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(user);

        jwtToken = jwtService.generateToken(user);

        testGroup = Group.builder()
                .groupName("Test Group")
                .password("validPassword")
                .groupCode("validGroupCode")
                .build();

        testGroupNotJoined = Group.builder()
                .groupName("groupNotJoined")
                .password("groupNotJoined")
                .groupCode("groupNotJoined")
                .build();

        testGroupNotOwner = Group.builder()
                .groupName("Test Group Not Owner")
                .password("validPassword")
                .groupCode("validGroupCodeNotOwner")
                .build();

        groupRepository.saveAll(List.of(testGroup, testGroupNotJoined, testGroupNotOwner));

        testLesson1 = Lesson.builder().title("Lesson 1").owner(user).build();
        testLesson2 = Lesson.builder().title("Lesson 2").owner(user).build();
        lessonRepository.saveAll(List.of(testLesson1, testLesson2));

        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(testGroup)
                .isOwner(true)
                .build();

        UserGroup userGroupNotOwner = UserGroup.builder()
                .user(user)
                .group(testGroupNotOwner)
                .isOwner(false)
                .build();

        userGroupRepository.saveAll(List.of(userGroup, userGroupNotOwner));
    }

    @AfterEach
    public void tearDown() {
        lessonRepository.deleteAll();
        userGroupRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testCreateGroupSuccessfully() throws Exception {
        String groupCreateRequest = "{\"groupName\": \"Test Group 2\", \"password\": \"groupPass123\"}";

        mockMvc.perform(post("/api/v1/group/createGroup")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(groupCreateRequest))
                .andExpect(status().isOk());

        Optional<Group> group = groupRepository.findByGroupName("Test Group 2");
        assertTrue(group.isPresent(), "Group should be created");
        assertEquals("Test Group 2", group.get().getGroupName(), "Group name should be correct");

        Optional<UserGroup> userGroup = userGroupRepository.findByUserAndGroup(userRepository.findByEmail("john.doe@example.com").orElseThrow(), group.get());
        assertTrue(userGroup.isPresent(), "User should be a member of the group");
        assertTrue(userGroup.get().isOwner(), "User should be the owner of the group");
    }

    @Test
    public void testCreateGroupWithoutAuthentication() throws Exception {
        String groupCreateRequest = "{\"groupName\": \"Test Group\", \"password\": \"groupPass123\"}";

        mockMvc.perform(post("/api/v1/group/createGroup")
                        .contentType("application/json")
                        .content(groupCreateRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateGroupWithInvalidData() throws Exception {
        String groupCreateRequest = "{\"groupName\": \"\", \"password\": \"\"}";

        mockMvc.perform(post("/api/v1/group/createGroup")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(groupCreateRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testJoinGroupSuccess() throws Exception {
        GroupJoinRequest groupJoinRequest = new GroupJoinRequest("groupNotJoined", "groupNotJoined");

        mockMvc.perform(post("/api/v1/group/join")
                        .cookie(new Cookie("accessToken", jwtToken)) // Token JWT
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(groupJoinRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("User successfully joined the group."));
    }

    @Test
    public void testJoinGroupWithInvalidCode() throws Exception {
        GroupJoinRequest groupJoinRequest = new GroupJoinRequest("invalidGroupCode", "validPassword");

        mockMvc.perform(post("/api/v1/group/join")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(groupJoinRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Group not found"));
    }

    @Test
    public void testJoinGroupWithWrongPassword() throws Exception {
        GroupJoinRequest groupJoinRequest = new GroupJoinRequest("groupNotJoined", "wrongPassword");

        mockMvc.perform(post("/api/v1/group/join")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(groupJoinRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid password"));
    }

    @Test
    public void testJoinGroupWhenAlreadyMember() throws Exception {
        GroupJoinRequest groupJoinRequest = new GroupJoinRequest("validGroupCode", "validPassword");

        mockMvc.perform(post("/api/v1/group/join")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(groupJoinRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User is already a member of this group"));
    }

    @Test
    public void testAddLessonsToGroupSuccessfully() throws Exception {
        List<Integer> lessonIds = List.of(testLesson1.getId(), testLesson2.getId());
        GroupAddLessonsRequest requestBody = new GroupAddLessonsRequest(lessonIds);

        mockMvc.perform(put("/api/v1/group/{groupId}/addLessons", testGroup.getId())
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().string("Lessons successfully added to group."));

        Group updatedGroup = groupRepository.findById(testGroup.getId()).orElseThrow();

        assertTrue(updatedGroup.getLessons().contains(testLesson1));
        assertTrue(updatedGroup.getLessons().contains(testLesson2));
    }

    @Test
    public void testAddLessonsToGroupWhenGroupDoesNotExist() throws Exception {
        List<Integer> lessonIds = List.of(testLesson1.getId(), testLesson2.getId());
        GroupAddLessonsRequest requestBody = new GroupAddLessonsRequest(lessonIds);

        mockMvc.perform(put("/api/v1/group/{groupId}/addLessons", 999)
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Group not found"));
    }

    @Test
    public void testAddLessonsToGroupWhenUserIsNotOwner() throws Exception {
        List<Integer> lessonIds = List.of(testLesson1.getId(), testLesson2.getId());
        GroupAddLessonsRequest requestBody = new GroupAddLessonsRequest(lessonIds);

        mockMvc.perform(put("/api/v1/group/{groupId}/addLessons", testGroupNotOwner.getId())
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Only the group owner can add lessons"));
    }

    @Test
    public void testAddLessonsToGroupWhenSomeLessonsDoNotExist() throws Exception {
        List<Integer> invalidLessonIds = List.of(999, 1000);
        GroupAddLessonsRequest requestBody = new GroupAddLessonsRequest(invalidLessonIds);

        mockMvc.perform(put("/api/v1/group/{groupId}/addLessons", testGroup.getId())
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Some lessons do not exist"));
    }
}