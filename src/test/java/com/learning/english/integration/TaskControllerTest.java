package com.learning.english.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.english.dto.TaskAddRequest;
import com.learning.english.dto.TaskCompleteRequest;
import com.learning.english.dto.TaskResponse;
import com.learning.english.dto.WordResponse;
import com.learning.english.models.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private TaskProgressRepository taskProgressRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private TaskType testTaskType;
    private TaskSubType testTaskSubType;
    private Lesson testLesson;
    private List<Word> testWords;
    private Group testGroup;
    private Task testTask;
    private TaskProgress testTaskProgress;
    private LessonProgress testLessonProgress;
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

        jwtToken = jwtService.generateToken(testUser);

        testGroup = groupRepository.save(
                Group.builder()
                        .groupName("Test Group")
                        .groupCode("test-group")
                        .build()
        );

        userGroupRepository.save(
                UserGroup.builder()
                        .user(testUser)
                        .group(testGroup)
                        .isOwner(true)
                        .build()
        );

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
                        .owner(testUser)
                        .groups(List.of(testGroup))
                        .build()
        );

        testWords = new ArrayList<>(List.of(
                Word.builder().word("cat").translation("kot").createdBy(testUser).build(),
                Word.builder().word("dog").translation("pies").createdBy(testUser).build()
        ));

        wordRepository.saveAll(testWords);

        testTask = taskRepository.save(
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
    }

    @AfterEach
    void tearDown() {
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
    void shouldAddTaskSuccessfully() throws Exception {
        taskRepository.deleteAll();

        TaskAddRequest request = new TaskAddRequest();
        request.setTaskTypeName(testTaskType.getTypeName());
        request.setTaskSubTypeName(testTaskSubType.getSubTypeName());
        request.setLessonId(List.of(testLesson.getId()));
        request.setWordIds(testWords.stream().map(Word::getId).toList());
        request.setContent("Translate the word");
        request.setCorrectAnswer("kot");
        request.setScore(10);

        mockMvc.perform(post("/api/v1/task/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk());

        List<Task> tasks = new ArrayList<>();
        taskRepository.findAll().forEach(tasks::add);
        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getContent()).isEqualTo("Translate the word");
        assertThat(task.getCorrectAnswer()).isEqualTo("kot");
        assertThat(task.getLesson().getId()).isEqualTo(testLesson.getId());
        assertThat(task.getWords()).hasSize(2);
        assertThat(task.getWords().stream().map(Word::getWord)).containsExactlyInAnyOrder("cat", "dog");
    }

    @Test
    void shouldReturnErrorForInvalidTaskType() throws Exception {
        TaskAddRequest request = new TaskAddRequest();
        request.setTaskTypeName("invalid-type");
        request.setTaskSubTypeName(testTaskSubType.getSubTypeName());
        request.setLessonId(List.of(testLesson.getId()));
        request.setWordIds(testWords.stream().map(Word::getId).toList());
        request.setContent("Translate the word");
        request.setCorrectAnswer("kot");
        request.setScore(10);

        mockMvc.perform(post("/api/v1/task/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetTaskByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/task/" + testTask.getId())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    TaskResponse response = objectMapper.readValue(json, TaskResponse.class);
                    assertThat(response.getId()).isEqualTo(testTask.getId());
                    assertThat(response.getTaskTypeName()).isEqualTo(testTaskType.getTypeName());
                    assertThat(response.getTaskSubTypeName()).isEqualTo(testTaskSubType.getSubTypeName());
                    assertThat(response.getContent()).isEqualTo("Translate the word");
                    assertThat(response.getCorrectAnswer()).isEqualTo("kot");
                    assertThat(response.getWords()).hasSize(2);
                    assertThat(response.getWords().stream().map(WordResponse::getWord))
                            .containsExactlyInAnyOrder("cat", "dog");
                    assertThat(response.isCompleted()).isFalse();
                });
    }

    @Test
    void shouldReturnErrorWhenTaskNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/task/999") // Non-existent ID
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnErrorWhenUserNotInTaskLessonGroup() throws Exception {
        User user = userRepository.save(
                User.builder()
                        .email("testuser2@example.com")
                        .firstName("Test")
                        .lastName("User")
                        .password("password")
                        .role(Role.USER)
                        .build()
        );

        String jwt = jwtService.generateToken(user);

        mockMvc.perform(get("/api/v1/task/" + testTask.getId())
                        .cookie(new Cookie("accessToken", jwt)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnTasksWhenUserHasAccess() throws Exception {
        mockMvc.perform(get("/api/v1/task/batch")
                        .param("taskIds", testTask.getId().toString())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(testTask.getId()));
    }

    @Test
    void shouldReturnTasksWithCompletedStatus() throws Exception {
        testLessonProgress = lessonProgressRepository.save(LessonProgress.builder()
                .lesson(testLesson)
                .user(testUser)
                .build()
        );

        testTaskProgress = taskProgressRepository.save(
                TaskProgress.builder()
                        .task(testTask)
                        .lessonProgress(testLessonProgress)
                        .completed(true)
                        .build()
        );

        mockMvc.perform(get("/api/v1/task/batch")
                        .param("taskIds", testTask.getId().toString())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].completed").value(true));
    }

    @Test
    void shouldReturnTasksWithNoProgress() throws Exception {
        mockMvc.perform(get("/api/v1/task/batch")
                        .param("taskIds", testTask.getId().toString())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].completed").value(false));
    }

    @Test
    void shouldReturnSuccessStatusAfterCompletingTask() throws Exception {
        TaskCompleteRequest validTaskCompleteRequest = TaskCompleteRequest.builder()
                .lessonId(testLesson.getId())
                .taskId(testTask.getId())
                .build();

        mockMvc.perform(post("/api/v1/task/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(validTaskCompleteRequest))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnPaginatedTasks() throws Exception {
        mockMvc.perform(get("/api/v1/task/all/owned/by/user")
                        .param("page", "0")
                        .param("size", "5")
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())  // Expect HTTP 200
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void shouldReturnEmptyListWhenNoTasksOwnedByUser() throws Exception {
        taskRepository.deleteAll();

        mockMvc.perform(get("/api/v1/task/all/owned/by/user")
                        .param("page", "0")
                        .param("size", "5")
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())  // Expect HTTP 200
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.page.totalPages").value(0));
    }

    @Test
    void shouldReturnCorrectTaskData() throws Exception {
        mockMvc.perform(get("/api/v1/task/all/owned/by/user")
                        .param("page", "0")
                        .param("size", "5")
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())  // Expect HTTP 200
                .andExpect(jsonPath("$._embedded.taskResponseList[0].id").value(testTask.getId()))
                .andExpect(jsonPath("$._embedded.taskResponseList[0].content").value(testTask.getContent()))
                .andExpect(jsonPath("$._embedded.taskResponseList[0].correctAnswer").value(testTask.getCorrectAnswer()))
                .andExpect(jsonPath("$._embedded.taskResponseList[0].words.length()").value(testTask.getWords().size()))
                .andExpect(jsonPath("$._embedded.taskResponseList[0].words[0].word").value(testTask.getWords().get(0).getWord()));
    }

    @Test
    void shouldReturnSuccessStatusWithCorrectPageMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/task/all/owned/by/user")
                        .param("page", "0")
                        .param("size", "5")
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void shouldDeleteTaskSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/task/{taskId}/delete", testTask.getId())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Task deleted successfully"));

        Optional<Task> deletedTask = taskRepository.findById(testTask.getId());
        assertFalse(deletedTask.isPresent());
    }

    @Test
    void shouldReturnForbiddenWhenUserNotOwner() throws Exception {
        User user = userRepository.save(
                User.builder()
                        .email("testuser2@example.com")
                        .firstName("Test")
                        .lastName("User")
                        .password("password")
                        .role(Role.USER)
                        .build()
        );

        String jwt = jwtService.generateToken(user);

        mockMvc.perform(delete("/api/v1/task/{taskId}/delete", testTask.getId())
                        .cookie(new Cookie("accessToken", jwt)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You are not authorized to delete this task."));
    }

    @Test
    void shouldReturnErrorWhenTaskNotFoundOnDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/task/{taskId}/delete", 999)
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Task not found"));
    }

    public static String asJsonString(final Object obj) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}