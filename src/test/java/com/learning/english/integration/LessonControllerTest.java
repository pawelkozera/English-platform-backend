package com.learning.english.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.english.dto.LessonAddRequest;
import com.learning.english.dto.LessonResponse;
import com.learning.english.dto.LessonUpdateRequest;
import com.learning.english.models.*;
import com.learning.english.repository.*;
import com.learning.english.service.JwtService;
import com.learning.english.service.LessonService;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LessonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LessonService lessonService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LessonRepository lessonRepository;

    private User testUser;
    private String jwtToken;
    private Group testGroup1;
    private Group testGroup2;

    @BeforeEach
    public void setUp() throws Exception {
        testUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(testUser);

        testGroup1 = Group.builder()
                .groupName("Group 1")
                .password("group1pass")
                .build();
        testGroup2 = Group.builder()
                .groupName("Group 2")
                .password("group2pass")
                .build();
        groupRepository.saveAll(List.of(testGroup1, testGroup2));

        UserGroup userGroup1 = UserGroup.builder()
                .user(testUser)
                .group(testGroup1)
                .isOwner(true)
                .build();

        UserGroup userGroup2 = UserGroup.builder()
                .user(testUser)
                .group(testGroup2)
                .isOwner(true)
                .build();

        userGroupRepository.saveAll(List.of(userGroup1, userGroup2));

        jwtToken = jwtService.generateToken(testUser);
    }

    @AfterEach
    public void tearDown() {
        lessonRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testAddLessonSuccessfully() throws Exception {
        LessonAddRequest lessonAddRequest = new LessonAddRequest("New Lesson", List.of(testGroup1.getId(), testGroup2.getId()));

        mockMvc.perform(post("/api/v1/lesson/add")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(lessonAddRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Lesson created successfully"));

        assertEquals(1, lessonRepository.count());
        Lesson lesson = lessonRepository.findAll().get(0);
        assertEquals("New Lesson", lesson.getTitle());
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {
        LessonAddRequest lessonAddRequest = new LessonAddRequest("Unauthorized Lesson", List.of(testGroup1.getId()));

        mockMvc.perform(post("/api/v1/lesson/add")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(lessonAddRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testNonOwnerAddingLesson() throws Exception {
        User nonOwnerUser = User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(nonOwnerUser);

        String nonOwnerJwtToken = jwtService.generateToken(nonOwnerUser);

        LessonAddRequest lessonAddRequest = new LessonAddRequest("Lesson by Non-Owner", List.of(testGroup1.getId()));

        mockMvc.perform(post("/api/v1/lesson/add")
                        .cookie(new Cookie("accessToken", nonOwnerJwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(lessonAddRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User is not the owner of one or more selected groups"));
    }

    @Test
    public void testGroupDoesNotExist() throws Exception {
        int nonExistentGroupId = 9999; // Assume this ID does not exist
        LessonAddRequest lessonAddRequest = new LessonAddRequest("Lesson with Invalid Group", List.of(nonExistentGroupId));

        mockMvc.perform(post("/api/v1/lesson/add")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(lessonAddRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No groups found for the provided IDs"));
    }

    @Test
    public void testEmptyGroupList() throws Exception {
        LessonAddRequest lessonAddRequest = new LessonAddRequest("Lesson with No Groups", List.of());

        mockMvc.perform(post("/api/v1/lesson/add")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(lessonAddRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No groups found for the provided IDs"));
    }

    @Test
    public void testDuplicateLessonTitlesInSameGroup() throws Exception {
        LessonAddRequest firstLessonRequest = new LessonAddRequest("Duplicate Title", List.of(testGroup1.getId()));
        mockMvc.perform(post("/api/v1/lesson/add")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(firstLessonRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Lesson created successfully"));

        LessonAddRequest duplicateLessonRequest = new LessonAddRequest("Duplicate Title", List.of(testGroup1.getId()));
        mockMvc.perform(post("/api/v1/lesson/add")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(duplicateLessonRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("A lesson with this title already exists in one of the groups"));
    }

    @Test
    public void testAddLessonWithSingleGroup() throws Exception {
        LessonAddRequest lessonAddRequest = new LessonAddRequest("Single Group Lesson", List.of(testGroup1.getId()));

        mockMvc.perform(post("/api/v1/lesson/add")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(lessonAddRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Lesson created successfully"));

        assertEquals(1, lessonRepository.count());
        Lesson lesson = lessonRepository.findAll().get(0);
        assertEquals("Single Group Lesson", lesson.getTitle());
        assertEquals(1, lesson.getGroups().size());
        assertEquals(testGroup1.getId(), lesson.getGroups().get(0).getId());
    }

    @Test
    public void testGetLessonsFromGroupSuccessfully() throws Exception {
        Lesson lesson = Lesson.builder()
                .title("Lesson 1")
                .groups(List.of(testGroup1))
                .owner(testUser)
                .build();
        lessonRepository.save(lesson);

        mockMvc.perform(get("/api/v1/lesson/all/from/group/" + testGroup1.getId())
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.lessonResponseList.length()").value(1))
                .andExpect(jsonPath("$._embedded.lessonResponseList[0].lessonId").value(lesson.getId()))
                .andExpect(jsonPath("$._embedded.lessonResponseList[0].title").value("Lesson 1"));
    }

    @Test
    public void testGetLessonsFromGroupUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/lesson/all/from/group/" + testGroup1.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetLessonsFromNonExistentGroup() throws Exception {
        int nonExistentGroupId = 9999;

        mockMvc.perform(get("/api/v1/lesson/all/from/group/" + nonExistentGroupId)
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Group not found"));
    }

    @Test
    public void testGetLessonsFromGroupUserNotMember() throws Exception {
        Group nonMemberGroup = Group.builder()
                .groupName("Non-Member Group")
                .password("nonMemberPass")
                .build();
        groupRepository.save(nonMemberGroup);

        mockMvc.perform(get("/api/v1/lesson/all/from/group/" + nonMemberGroup.getId())
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User is not a member of the group"));
    }

    @Test
    public void testGetLessonsFromGroupPagination() throws Exception {
        List<Lesson> lessons = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Lesson lesson = Lesson.builder()
                    .title("Lesson " + i)
                    .groups(List.of(testGroup1))
                    .owner(testUser)
                    .build();
            lessons.add(lesson);
        }

        lessonRepository.saveAll(lessons);

        mockMvc.perform(get("/api/v1/lesson/all/from/group/" + testGroup1.getId())
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.lessonResponseList.length()").value(10))
                .andExpect(jsonPath("$._embedded.lessonResponseList[0].title").value("Lesson 1"))
                .andExpect(jsonPath("$._embedded.lessonResponseList[9].title").value("Lesson 10"));

        mockMvc.perform(get("/api/v1/lesson/all/from/group/" + testGroup1.getId())
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.lessonResponseList.length()").value(5))
                .andExpect(jsonPath("$._embedded.lessonResponseList[0].title").value("Lesson 11"))
                .andExpect(jsonPath("$._embedded.lessonResponseList[4].title").value("Lesson 15"));
    }

    @Test
    public void testGetLessonsNotAssignedToGroup() throws Exception {
        List<Lesson> lessons = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Lesson lesson = Lesson.builder()
                    .title("Unassigned Lesson " + i)
                    .groups(new ArrayList<>())
                    .owner(testUser)
                    .build();
            lessons.add(lesson);
        }
        lessonRepository.saveAll(lessons);

        mockMvc.perform(get("/api/v1/lesson/all/not/assigned/to/group/" + testGroup1.getId())
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.lessonResponseList.length()").value(5))
                .andExpect(jsonPath("$._embedded.lessonResponseList[0].title").value("Unassigned Lesson 1"))
                .andExpect(jsonPath("$._embedded.lessonResponseList[4].title").value("Unassigned Lesson 5"))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(1))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    public void testGetLessonsOwnedByUser() throws Exception {
        List<Lesson> lessons = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Lesson lesson = Lesson.builder()
                    .title("Owned Lesson " + i)
                    .groups(List.of(testGroup1, testGroup2))
                    .owner(testUser)
                    .build();
            lessons.add(lesson);
        }
        lessonRepository.saveAll(lessons);

        mockMvc.perform(get("/api/v1/lesson/all/owned/by/user")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.lessonWithGroupsResponseList.length()").value(5))
                .andExpect(jsonPath("$._embedded.lessonWithGroupsResponseList[0].title").value("Owned Lesson 1"))
                .andExpect(jsonPath("$._embedded.lessonWithGroupsResponseList[4].title").value("Owned Lesson 5"))
                .andExpect(jsonPath("$._embedded.lessonWithGroupsResponseList[0].groupIds.length()").value(2))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(1))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    public void testUpdateLesson_Success() throws Exception {
        Lesson lesson = Lesson.builder()
                .title("Original Lesson Title")
                .groups(List.of(testGroup1))
                .owner(testUser)
                .build();
        lessonRepository.save(lesson);

        LessonUpdateRequest updateRequest = new LessonUpdateRequest("Updated Lesson Title", List.of(testGroup2.getId()));

        mockMvc.perform(put("/api/v1/lesson/" + lesson.getId() + "/update")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Lesson updated successfully"));

        Lesson updatedLesson = lessonRepository.findById(lesson.getId()).orElseThrow();
        assertEquals("Updated Lesson Title", updatedLesson.getTitle());
        assertEquals(1, updatedLesson.getGroups().size());
        assertEquals(testGroup2.getId(), updatedLesson.getGroups().get(0).getId());
    }

    @Test
    public void testUpdateLesson_Fail_NotOwner() throws Exception {
        User otherUser = User.builder()
                .firstName("Other")
                .lastName("Other")
                .email("Other.smith@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(otherUser);

        String otherJwt = jwtService.generateToken(otherUser);

        Lesson lesson = Lesson.builder()
                .title("Original Lesson Title")
                .groups(List.of(testGroup1))
                .owner(otherUser)
                .build();
        lessonRepository.save(lesson);

        LessonUpdateRequest updateRequest = new LessonUpdateRequest("Updated Lesson Title", List.of(testGroup2.getId()));

        mockMvc.perform(put("/api/v1/lesson/" + lesson.getId() + "/update")
                        .cookie(new Cookie("accessToken", otherJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to update lesson"));
    }

    @Test
    public void testUpdateLesson_Fail_LessonNotFound() throws Exception {
        LessonUpdateRequest updateRequest = new LessonUpdateRequest("Updated Lesson Title", List.of(testGroup2.getId()));

        mockMvc.perform(put("/api/v1/lesson/999/update")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to update lesson"));
    }

    @Test
    public void testDeleteLesson_Success() throws Exception {
        Lesson lesson = Lesson.builder()
                .title("Lesson to be deleted")
                .owner(testUser)
                .build();
        lessonRepository.save(lesson);

        mockMvc.perform(delete("/api/v1/lesson/" + lesson.getId() + "/delete")
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Lesson deleted successfully"));

        Optional<Lesson> deletedLesson = lessonRepository.findById(lesson.getId());
        Assertions.assertFalse(deletedLesson.isPresent());
    }

    @Test
    public void testDeleteLesson_Fail_LessonNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/lesson/999/delete")
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Lesson not found"));
    }

    @Test
    public void testDeleteLesson_Fail_NotOwner() throws Exception {
        User otherUser = User.builder()
                .firstName("Other")
                .lastName("Other")
                .email("Other.smith@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(otherUser);

        String otherJwt = jwtService.generateToken(otherUser);

        Lesson lesson = Lesson.builder()
                .title("Lesson to be deleted")
                .owner(testUser)
                .build();
        lessonRepository.save(lesson);

        mockMvc.perform(delete("/api/v1/lesson/" + lesson.getId() + "/delete")
                        .cookie(new Cookie("accessToken", otherJwt)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You are not authorized to delete this lesson"));

        Optional<Lesson> notDeletedLesson = lessonRepository.findById(lesson.getId());
        assertTrue(notDeletedLesson.isPresent());
    }
}