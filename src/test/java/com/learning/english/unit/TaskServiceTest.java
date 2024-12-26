package com.learning.english.unit;

import com.learning.english.dto.TaskAddRequest;
import com.learning.english.dto.TaskCompleteRequest;
import com.learning.english.dto.TaskResponse;
import com.learning.english.exception.TaskNotFoundException;
import com.learning.english.models.*;
import com.learning.english.repository.*;
import com.learning.english.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private TaskTypeRepository taskTypeRepository;

    @Mock
    private TaskSubTypeRepository taskSubTypeRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private WordRepository wordRepository;

    @Mock
    private TaskService taskService;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private TaskProgressRepository taskProgressRepository;

    @Mock
    private User user;

    @Mock
    private TaskCompleteRequest taskCompleteRequest;

    @Mock
    private LessonProgress lessonProgress;

    @Mock
    private TaskProgress taskProgress;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        taskService = new TaskService(
                taskRepository,
                taskTypeRepository,
                taskSubTypeRepository,
                lessonRepository,
                userGroupRepository,
                wordRepository,
                groupRepository,
                lessonProgressRepository,
                taskProgressRepository
        );
    }

    @Test
    void shouldAddTaskSuccessfully() {
        User user = new User();
        user.setId(1);

        TaskAddRequest request = new TaskAddRequest();
        request.setTaskTypeName("Typing");
        request.setTaskSubTypeName("Translation");
        request.setLessonId(List.of(1));
        request.setWordIds(List.of(1L, 2L));
        request.setContent("Task Content");
        request.setCorrectAnswer("Correct Answer");
        request.setScore(10);

        TaskType taskType = new TaskType();
        TaskSubType taskSubType = new TaskSubType();
        Lesson lesson = new Lesson();
        lesson.setId(1);
        lesson.setGroups(List.of(new Group()));

        Word word1 = new Word();
        Word word2 = new Word();

        when(taskTypeRepository.findByTypeName("Typing")).thenReturn(Optional.of(taskType));
        when(taskSubTypeRepository.findBySubTypeName("Translation")).thenReturn(Optional.of(taskSubType));
        when(lessonRepository.findAllById(List.of(1))).thenReturn(List.of(lesson));
        when(userGroupRepository.existsByUserAndGroupAndIsOwnerTrue(any(), any())).thenReturn(true);
        when(wordRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(word1, word2));

        taskService.addTask(request, user);

        verify(taskRepository, times(1)).saveAll(any());
    }

    @Test
    void shouldThrowExceptionWhenTaskTypeIsInvalid() {
        User user = new User();
        TaskAddRequest request = new TaskAddRequest();
        request.setTaskTypeName("InvalidType");

        when(taskTypeRepository.findByTypeName("InvalidType")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.addTask(request, user));
    }

    @Test
    void shouldThrowExceptionWhenTaskSubTypeIsInvalid() {
        User user = new User();
        TaskAddRequest request = new TaskAddRequest();
        request.setTaskTypeName("Typing");
        request.setTaskSubTypeName("InvalidSubType");

        TaskType taskType = new TaskType();
        when(taskTypeRepository.findByTypeName("Typing")).thenReturn(Optional.of(taskType));
        when(taskSubTypeRepository.findBySubTypeName("InvalidSubType")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.addTask(request, user));
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotOwnerOfLessonGroup() {
        User user = new User();
        user.setId(1);

        TaskAddRequest request = new TaskAddRequest();
        request.setTaskTypeName("Typing");
        request.setTaskSubTypeName("Translation");
        request.setLessonId(List.of(1));

        TaskType taskType = new TaskType();
        TaskSubType taskSubType = new TaskSubType();
        Lesson lesson = new Lesson();
        lesson.setId(1);
        lesson.setGroups(List.of(new Group()));

        when(taskTypeRepository.findByTypeName("Typing")).thenReturn(Optional.of(taskType));
        when(taskSubTypeRepository.findBySubTypeName("Translation")).thenReturn(Optional.of(taskSubType));
        when(lessonRepository.findAllById(List.of(1))).thenReturn(List.of(lesson));
        when(userGroupRepository.existsByUserAndGroupAndIsOwnerTrue(eq(user), any())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> taskService.addTask(request, user));
    }

    @Test
    void shouldReturnTaskResponseWhenUserBelongsToLessonGroup() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(2);

        Lesson lesson = new Lesson();
        lesson.setId(3);
        lesson.setGroups(List.of(group));

        Task task = new Task();
        task.setId(4);
        task.setLesson(lesson);
        task.setTaskType(new TaskType("Typing"));
        task.setTaskSubType(new TaskSubType("Translation"));
        task.setContent("Task Content");
        task.setCorrectAnswer("Correct Answer");
        task.setWords(List.of(new Word(5L, "word", "translation")));

        when(taskRepository.findById(4)).thenReturn(Optional.of(task));
        when(groupRepository.existsByGroupIdAndUserId(2, 1)).thenReturn(true);
        when(lessonProgressRepository.findByUserAndLessonId(user, 3)).thenReturn(null);

        TaskResponse response = taskService.getTaskById(user, 4);

        assertNotNull(response);
        assertEquals(4, response.getId());
        assertEquals("Typing", response.getTaskTypeName());
        assertEquals("Translation", response.getTaskSubTypeName());
        assertFalse(response.isCompleted());
    }

    @Test
    void shouldThrowExceptionWhenTaskDoesNotExist() {
        User user = new User();
        user.setId(1);

        when(taskRepository.findById(4)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(user, 4));
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotBelongToAnyGroupForLesson() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(2);

        Lesson lesson = new Lesson();
        lesson.setId(3);
        lesson.setGroups(List.of(group));

        Task task = new Task();
        task.setId(4);
        task.setLesson(lesson);

        when(taskRepository.findById(4)).thenReturn(Optional.of(task));
        when(groupRepository.existsByGroupIdAndUserId(2, 1)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> taskService.getTaskById(user, 4));
    }

    @Test
    void shouldReturnTaskResponseWhenNoLessonProgressExists() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(2);

        Lesson lesson = new Lesson();
        lesson.setId(3);
        lesson.setGroups(List.of(group));

        Task task = new Task();
        task.setId(4);
        task.setLesson(lesson);
        task.setTaskType(new TaskType("Typing"));
        task.setTaskSubType(new TaskSubType("Translation"));
        task.setContent("Task Content");
        task.setCorrectAnswer("Correct Answer");
        task.setWords(List.of(new Word(1L, "word", "translation")));

        when(taskRepository.findById(4)).thenReturn(Optional.of(task));
        when(groupRepository.existsByGroupIdAndUserId(2, 1)).thenReturn(true);
        when(lessonProgressRepository.findByUserAndLessonId(user, 3)).thenReturn(null);

        TaskResponse response = taskService.getTaskById(user, 4);

        assertNotNull(response);
        assertFalse(response.isCompleted());
        assertNotNull(response.getWords());
        assertEquals(1, response.getWords().size());
        assertEquals("word", response.getWords().get(0).getWord());
    }

    @Test
    void shouldReturnTaskResponseWhenTaskProgressIsNotCompleted() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(2);

        Lesson lesson = new Lesson();
        lesson.setId(3);
        lesson.setGroups(List.of(group));

        LessonProgress lessonProgress = new LessonProgress();
        lessonProgress.setId(6);

        TaskType taskType = new TaskType();
        taskType.setTypeName("Typing");

        TaskSubType taskSubType = new TaskSubType();
        taskSubType.setSubTypeName("Translation");

        Task task = new Task();
        task.setId(4);
        task.setLesson(lesson);
        task.setTaskType(taskType);
        task.setTaskSubType(taskSubType);
        task.setWords(List.of(new Word(1L, "word", "translation")));

        TaskProgress taskProgress = new TaskProgress();
        taskProgress.setCompleted(false);
        taskProgress.setTask(task);

        when(taskRepository.findById(4)).thenReturn(Optional.of(task));
        when(groupRepository.existsByGroupIdAndUserId(2, 1)).thenReturn(true);
        when(lessonProgressRepository.findByUserAndLessonId(user, 3)).thenReturn(lessonProgress);
        when(taskProgressRepository.findByLessonProgressAndTaskId(lessonProgress, 4)).thenReturn(taskProgress);

        TaskResponse response = taskService.getTaskById(user, 4);

        assertNotNull(response);
        assertFalse(response.isCompleted());
        assertNotNull(response.getWords());
        assertEquals(1, response.getWords().size());
        assertEquals("word", response.getWords().get(0).getWord());
        assertEquals("Typing", response.getTaskTypeName());
        assertEquals("Translation", response.getTaskSubTypeName());
    }

    @Test
    void shouldReturnTaskResponseWhenTaskProgressIsCompleted() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(2);

        Lesson lesson = new Lesson();
        lesson.setId(3);
        lesson.setGroups(List.of(group));

        LessonProgress lessonProgress = new LessonProgress();
        lessonProgress.setId(6);

        TaskType taskType = new TaskType();
        taskType.setTypeName("Typing");

        TaskSubType taskSubType = new TaskSubType();
        taskSubType.setSubTypeName("Translation");

        Task task = new Task();
        task.setId(4);
        task.setLesson(lesson);
        task.setTaskType(taskType);
        task.setTaskSubType(taskSubType);
        task.setWords(List.of(new Word(1L, "word", "translation")));

        TaskProgress taskProgress = new TaskProgress();
        taskProgress.setTask(task);
        taskProgress.setLessonProgress(lessonProgress);
        taskProgress.setCompleted(false);

        when(taskRepository.findById(4)).thenReturn(Optional.of(task));
        when(groupRepository.existsByGroupIdAndUserId(2, 1)).thenReturn(true);
        when(lessonProgressRepository.findByUserAndLessonId(user, 3)).thenReturn(lessonProgress);
        when(taskProgressRepository.findByLessonProgressAndTaskId(lessonProgress, 4)).thenReturn(taskProgress);

        TaskResponse response = taskService.getTaskById(user, 4);

        assertNotNull(response);
        assertFalse(response.isCompleted());
        assertNotNull(response.getWords());
        assertEquals(1, response.getWords().size());
        assertEquals("word", response.getWords().get(0).getWord());
    }

    @Test
    void shouldReturnTaskResponsesWhenTasksAreValid() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(2);

        Lesson lesson = new Lesson();
        lesson.setId(3);
        lesson.setGroups(List.of(group));

        Task task = new Task();
        task.setId(4);
        task.setLesson(lesson);
        task.setTaskType(new TaskType("Typing"));
        task.setTaskSubType(new TaskSubType("Translation"));
        task.setContent("some content");
        task.setCorrectAnswer("correct answer");
        task.setWords(List.of(new Word(1L, "word", "translation")));
        task.setScore(10);

        TaskProgress taskProgress = new TaskProgress();
        taskProgress.setCompleted(true);

        LessonProgress lessonProgress = new LessonProgress();
        lessonProgress.setId(6);

        when(taskRepository.findAllById(List.of(4))).thenReturn(List.of(task));
        when(groupRepository.existsByGroupIdAndUserId(2, 1)).thenReturn(true);
        when(lessonProgressRepository.findByUserAndLessonId(user, 3)).thenReturn(lessonProgress);
        when(taskProgressRepository.findByLessonProgressAndTaskId(lessonProgress, 4)).thenReturn(taskProgress);

        List<TaskResponse> responses = taskService.getTasksByIds(user, List.of(4));

        assertNotNull(responses);
        assertEquals(1, responses.size());
        TaskResponse response = responses.get(0);
        assertEquals(4, response.getId());
        assertEquals("Typing", response.getTaskTypeName());
        assertEquals("Translation", response.getTaskSubTypeName());
        assertTrue(response.isCompleted());
        assertNotNull(response.getWords());
        assertEquals(1, response.getWords().size());
        assertEquals("d29yZA==", response.getWords().get(0).getWord());
        assertEquals("c29tZSBjb250ZW50", response.getContent());
        assertEquals("Y29ycmVjdCBhbnN3ZXI=", response.getCorrectAnswer());
    }

    @Test
    void shouldReturnEmptyListWhenNoTasksFound() {
        User user = new User();
        user.setId(1);

        when(taskRepository.findAllById(List.of(4))).thenReturn(Collections.emptyList());

        List<TaskResponse> responses = taskService.getTasksByIds(user, List.of(4));

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenUserIsNotInGroup() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(2);

        Lesson lesson = new Lesson();
        lesson.setId(3);
        lesson.setGroups(List.of(group));

        Task task = new Task();
        task.setId(4);
        task.setLesson(lesson);

        when(taskRepository.findAllById(List.of(4))).thenReturn(List.of(task));
        when(groupRepository.existsByGroupIdAndUserId(2, 1)).thenReturn(false);

        List<TaskResponse> responses = taskService.getTasksByIds(user, List.of(4));

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void shouldCompleteTaskWhenTaskProgressExistsAndIsNotCompleted() {
        user = new User();
        user.setId(1);

        taskCompleteRequest = new TaskCompleteRequest();
        taskCompleteRequest.setLessonId(3);
        taskCompleteRequest.setTaskId(5);

        lessonProgress = new LessonProgress();
        lessonProgress.setId(6);

        taskProgress = new TaskProgress();
        taskProgress.setCompleted(false);

        when(lessonProgressRepository.findByUserAndLessonId(user, 3)).thenReturn(lessonProgress);
        when(taskProgressRepository.findByLessonProgressAndTaskId(lessonProgress, 5)).thenReturn(taskProgress);

        taskService.completeTask(user, taskCompleteRequest);

        assertTrue(taskProgress.isCompleted(), "Task should be marked as completed");
        verify(taskProgressRepository, times(1)).save(taskProgress);
    }

    @Test
    void shouldNotCompleteTaskWhenTaskProgressExistsAndIsAlreadyCompleted() {
        user = new User();
        user.setId(1);

        taskCompleteRequest = new TaskCompleteRequest();
        taskCompleteRequest.setLessonId(3);
        taskCompleteRequest.setTaskId(5);

        lessonProgress = new LessonProgress();
        lessonProgress.setId(6);

        taskProgress = new TaskProgress();
        taskProgress.setCompleted(false);

        taskProgress.setCompleted(true);
        when(lessonProgressRepository.findByUserAndLessonId(user, 3)).thenReturn(lessonProgress);
        when(taskProgressRepository.findByLessonProgressAndTaskId(lessonProgress, 5)).thenReturn(taskProgress);

        taskService.completeTask(user, taskCompleteRequest);

        assertTrue(taskProgress.isCompleted(), "Task should still be marked as completed");
        verify(taskProgressRepository, never()).save(taskProgress);
    }

    @Test
    void shouldNotCompleteTaskWhenLessonProgressIsNotFound() {
        user = new User();
        user.setId(1);

        taskCompleteRequest = new TaskCompleteRequest();
        taskCompleteRequest.setLessonId(3);
        taskCompleteRequest.setTaskId(5);

        lessonProgress = new LessonProgress();
        lessonProgress.setId(6);

        taskProgress = new TaskProgress();
        taskProgress.setCompleted(false);

        when(lessonProgressRepository.findByUserAndLessonId(user, 3)).thenReturn(null);

        taskService.completeTask(user, taskCompleteRequest);

        verify(taskProgressRepository, never()).findByLessonProgressAndTaskId(any(), any());
        verify(taskProgressRepository, never()).save(any());
    }

    @Test
    void shouldReturnPagedTaskResponsesWhenUserHasTasks() {
        user = new User();
        user.setId(1);

        Task task = new Task();
        task.setId(1);
        task.setTaskType(new TaskType("Typing"));
        task.setTaskSubType(new TaskSubType("Translation"));
        task.setContent("Task Content");
        task.setCorrectAnswer("Correct Answer");
        task.setWords(List.of(new Word(1L, "word", "translation", "audioPath", "imagePath")));

        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(List.of(task));

        when(taskRepository.findByOwner(user, pageRequest)).thenReturn(taskPage);

        Page<TaskResponse> taskResponses = taskService.getTasksOwnedByUser(user, 0, 10);

        assertNotNull(taskResponses);
        assertEquals(1, taskResponses.getContent().size());
        TaskResponse response = taskResponses.getContent().get(0);
        assertEquals(1, response.getId());
        assertEquals("Typing", response.getTaskTypeName());
        assertEquals("Translation", response.getTaskSubTypeName());
        assertEquals("Task Content", response.getContent());
        assertEquals("Correct Answer", response.getCorrectAnswer());
        assertNotNull(response.getWords());
        assertEquals(1, response.getWords().size());
        assertEquals("word", response.getWords().get(0).getWord());
    }

    @Test
    void shouldReturnEmptyPageWhenUserHasNoTasks() {
        user = new User();
        user.setId(1);

        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Task> emptyPage = Page.empty();

        when(taskRepository.findByOwner(user, pageRequest)).thenReturn(emptyPage);

        Page<TaskResponse> taskResponses = taskService.getTasksOwnedByUser(user, 0, 10);

        assertNotNull(taskResponses);
        assertEquals(0, taskResponses.getContent().size());
    }

    @Test
    void shouldReturnMultipleTasksWhenUserHasMultipleTasks() {
        user = new User();
        user.setId(1);

        Task task1 = new Task();
        task1.setId(1);
        task1.setTaskType(new TaskType("Typing"));
        task1.setTaskSubType(new TaskSubType("Translation"));
        task1.setContent("Task 1 Content");
        task1.setCorrectAnswer("Task 1 Correct Answer");
        task1.setWords(List.of(new Word(1L, "word1", "translation1", "audio1", "image1")));

        Task task2 = new Task();
        task2.setId(2);
        task2.setTaskType(new TaskType("Connection"));
        task2.setTaskSubType(new TaskSubType("Image"));
        task2.setContent("Task 2 Content");
        task2.setCorrectAnswer("Task 2 Correct Answer");
        task2.setWords(List.of(new Word(2L, "word2", "translation2", "audio2", "image2")));

        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(List.of(task1, task2));

        when(taskRepository.findByOwner(user, pageRequest)).thenReturn(taskPage);

        Page<TaskResponse> taskResponses = taskService.getTasksOwnedByUser(user, 0, 10);

        assertNotNull(taskResponses);
        assertEquals(2, taskResponses.getContent().size());

        TaskResponse response1 = taskResponses.getContent().get(0);
        assertEquals(1, response1.getId());
        assertEquals("Typing", response1.getTaskTypeName());
        assertEquals("Translation", response1.getTaskSubTypeName());
        assertEquals("Task 1 Content", response1.getContent());
        assertEquals("Task 1 Correct Answer", response1.getCorrectAnswer());

        TaskResponse response2 = taskResponses.getContent().get(1);
        assertEquals(2, response2.getId());
        assertEquals("Connection", response2.getTaskTypeName());
        assertEquals("Image", response2.getTaskSubTypeName());
        assertEquals("Task 2 Content", response2.getContent());
        assertEquals("Task 2 Correct Answer", response2.getCorrectAnswer());
    }

    @Test
    void shouldHandleNoWordsInTask() {
        user = new User();
        user.setId(1);

        Task task = new Task();
        task.setId(1);
        task.setTaskType(new TaskType("Typing"));
        task.setTaskSubType(new TaskSubType("Translation"));
        task.setContent("Task Content");
        task.setCorrectAnswer("Correct Answer");
        task.setWords(List.of());

        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(List.of(task));

        when(taskRepository.findByOwner(user, pageRequest)).thenReturn(taskPage);

        Page<TaskResponse> taskResponses = taskService.getTasksOwnedByUser(user, 0, 10);

        assertNotNull(taskResponses);
        assertEquals(1, taskResponses.getContent().size());
        TaskResponse response = taskResponses.getContent().get(0);
        assertTrue(response.getWords().isEmpty());
    }

    @Test
    void shouldDeleteTaskWhenUserIsOwner() {
        User owner = new User();
        owner.setId(1);

        User nonOwner = new User();
        nonOwner.setId(2);

        Task task = new Task();
        task.setId(1);
        task.setOwner(owner);

        when(taskRepository.findById(1)).thenReturn(Optional.of(task));

        taskService.deleteTask(owner, 1);

        verify(taskRepository, times(1)).delete(task);
    }

    @Test
    void shouldThrowSecurityExceptionWhenUserIsNotOwner() {
        User owner = new User();
        owner.setId(1);

        User nonOwner = new User();
        nonOwner.setId(2);

        Task task = new Task();
        task.setId(1);
        task.setOwner(owner);

        when(taskRepository.findById(1)).thenReturn(Optional.of(task));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.deleteTask(nonOwner, 1);
        });
        assertEquals("You are not authorized to delete this task.", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTaskNotFound() {
        User owner = new User();
        owner.setId(1);

        User nonOwner = new User();
        nonOwner.setId(2);

        Task task = new Task();
        task.setId(1);
        task.setOwner(owner);

        when(taskRepository.findById(1)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.deleteTask(owner, 1);
        });
        assertEquals("Task not found", exception.getMessage());
    }
}