package com.learning.english.unit;

import com.learning.english.dto.*;
import com.learning.english.models.*;
import com.learning.english.repository.*;
import com.learning.english.service.LessonService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LessonServiceTest {

    @InjectMocks
    private LessonService lessonService;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskProgressRepository taskProgressRepository;

    private User mockUser;
    private Group mockGroup;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setEmail("test@example.com");

        mockGroup = new Group();
        mockGroup.setId(1);
        mockGroup.setGroupName("Test Group");
        mockGroup.setLessons(List.of());
    }

    @Test
    void shouldAddLessonSuccessfully() {
        LessonAddRequest request = new LessonAddRequest();
        request.setTitle("New Lesson");
        request.setGroupId(List.of(1));

        Group mockGroup = mock(Group.class);
        List<Lesson> lessons = new ArrayList<>();
        when(mockGroup.getLessons()).thenReturn(lessons);

        when(groupRepository.findAllById(request.getGroupId())).thenReturn(List.of(mockGroup));
        when(userGroupRepository.existsByUserAndGroupAndIsOwnerTrue(mockUser, mockGroup)).thenReturn(true);

        lessonService.addLesson(request, mockUser);

        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository).save(lessonCaptor.capture());

        Lesson savedLesson = lessonCaptor.getValue();
        assertEquals("New Lesson", savedLesson.getTitle());
        assertEquals(mockUser, savedLesson.getOwner());
        assertTrue(lessons.contains(savedLesson));
    }


    @Test
    void shouldThrowExceptionWhenNoGroupsFound() {
        LessonAddRequest request = new LessonAddRequest();
        request.setTitle("New Lesson");
        request.setGroupId(List.of(1));

        when(groupRepository.findAllById(request.getGroupId())).thenReturn(Collections.emptyList());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.addLesson(request, mockUser)
        );
        assertEquals("No groups found for the provided IDs", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotOwnerForAllGroups() {
        LessonAddRequest request = new LessonAddRequest();
        request.setTitle("New Lesson");
        request.setGroupId(List.of(1));

        when(groupRepository.findAllById(request.getGroupId())).thenReturn(List.of(mockGroup));
        when(userGroupRepository.existsByUserAndGroupAndIsOwnerTrue(mockUser, mockGroup)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.addLesson(request, mockUser)
        );
        assertEquals("User is not the owner of one or more selected groups", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenLessonWithSameTitleExists() {
        LessonAddRequest request = new LessonAddRequest();
        request.setTitle("Duplicate Lesson");
        request.setGroupId(List.of(1));

        Lesson existingLesson = new Lesson();
        existingLesson.setTitle("Duplicate Lesson");

        mockGroup.setLessons(List.of(existingLesson));

        when(groupRepository.findAllById(request.getGroupId())).thenReturn(List.of(mockGroup));
        when(userGroupRepository.existsByUserAndGroupAndIsOwnerTrue(mockUser, mockGroup)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.addLesson(request, mockUser)
        );
        assertEquals("A lesson with this title already exists in one of the groups", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenGroupNotFound() {
        Integer groupId = 1;
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            lessonService.getLessonsFromGroup(mockUser, groupId, 0, 10);
        });
    }

    @Test
    void shouldThrowExceptionWhenUserNotAMember() {
        Integer groupId = 1;
        Group group = mock(Group.class);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.existsByUserAndGroup(mockUser, group)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            lessonService.getLessonsFromGroup(mockUser, groupId, 0, 10);
        });
    }

    @Test
    void shouldReturnLessonsWhenUserIsMember() {
        Integer groupId = 1;
        Group group = mock(Group.class);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.existsByUserAndGroup(mockUser, group)).thenReturn(true);

        Lesson lesson1 = new Lesson();
        lesson1.setId(1);
        lesson1.setTitle("Lesson 1");
        Lesson lesson2 = new Lesson();
        lesson2.setId(2);
        lesson2.setTitle("Lesson 2");
        Page<Lesson> lessonPage = new PageImpl<>(List.of(lesson1, lesson2), PageRequest.of(0, 10), 2);

        when(lessonRepository.findAllByGroupsContaining(group, PageRequest.of(0, 10))).thenReturn(lessonPage);

        Page<LessonResponse> response = lessonService.getLessonsFromGroup(mockUser, groupId, 0, 10);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals("Lesson 1", response.getContent().get(0).getTitle());
        assertEquals("Lesson 2", response.getContent().get(1).getTitle());
    }

    @Test
    void shouldReturnEmptyPageWhenUserHasNoLessons() {
        int page = 0;
        int size = 10;
        PageRequest pageable = PageRequest.of(page, size);
        when(lessonRepository.findLessonsByOwner(mockUser, pageable)).thenReturn(Page.empty());

        Page<LessonResponse> response = lessonService.getLessonsNotAssignedToGroup(mockUser, 1, page, size);

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void shouldReturnLessonsWhenUserHasLessons() {
        int page = 0;
        int size = 10;
        Lesson lesson1 = new Lesson();
        lesson1.setId(1);
        lesson1.setTitle("Lesson 1");
        Lesson lesson2 = new Lesson();
        lesson2.setId(2);
        lesson2.setTitle("Lesson 2");

        List<Lesson> lessons = List.of(lesson1, lesson2);
        Page<Lesson> lessonPage = new PageImpl<>(lessons, PageRequest.of(page, size), lessons.size());
        when(lessonRepository.findLessonsByOwner(mockUser, PageRequest.of(page, size))).thenReturn(lessonPage);

        Page<LessonResponse> response = lessonService.getLessonsNotAssignedToGroup(mockUser, 1, page, size);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals("Lesson 1", response.getContent().get(0).getTitle());
        assertEquals("Lesson 2", response.getContent().get(1).getTitle());
    }

    @Test
    void shouldReturnPaginatedLessons() {
        int page = 1;
        int size = 2;
        Lesson lesson1 = new Lesson();
        lesson1.setId(1);
        lesson1.setTitle("Lesson 1");
        Lesson lesson2 = new Lesson();
        lesson2.setId(2);
        lesson2.setTitle("Lesson 2");
        Lesson lesson3 = new Lesson();
        lesson3.setId(3);
        lesson3.setTitle("Lesson 3");

        List<Lesson> lessons = List.of(lesson1, lesson2, lesson3);
        Page<Lesson> lessonPage = new PageImpl<>(lessons.subList(2, 3), PageRequest.of(page, size), lessons.size());
        when(lessonRepository.findLessonsByOwner(mockUser, PageRequest.of(page, size))).thenReturn(lessonPage);

        Page<LessonResponse> response = lessonService.getLessonsNotAssignedToGroup(mockUser, 1, page, size);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Lesson 3", response.getContent().get(0).getTitle());
    }

    @Test
    void shouldThrowExceptionWhenGroupNotFoundForDisplay() {
        int groupId = 1;
        int page = 0;
        int size = 10;
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            lessonService.getLessonsForDisplay(mockUser, groupId, page, size);
        });
    }

    @Test
    void shouldReturnLessonsForDisplayWithZeroCompletedTasksWhenNoProgress() {
        // Given
        int groupId = 1;
        int page = 0;
        int size = 10;
        Lesson lesson = new Lesson();
        lesson.setId(1);
        lesson.setTitle("Lesson 1");
        List<Lesson> lessons = List.of(lesson);
        Page<Lesson> lessonPage = new PageImpl<>(lessons);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
        when(lessonRepository.findAllByGroupsContaining(mockGroup, PageRequest.of(page, size)))
                .thenReturn(lessonPage);
        when(lessonProgressRepository.findByUserAndLesson(mockUser, lesson)).thenReturn(Optional.empty());

        Page<LessonsDisplayResponse> response = lessonService.getLessonsForDisplay(mockUser, groupId, page, size);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        LessonsDisplayResponse displayResponse = response.getContent().get(0);
        assertEquals(1, displayResponse.getLessonId());
        assertEquals("Lesson 1", displayResponse.getTitle());
        assertEquals(0, displayResponse.getCompletedTasks());
        assertEquals(0, displayResponse.getTotalTasks());
    }

    @Test
    void shouldReturnLessonsForDisplayWithCompletedTasks() {
        int groupId = 1;
        int page = 0;
        int size = 10;
        Lesson lesson = new Lesson();
        lesson.setId(1);
        lesson.setTitle("Lesson 1");

        Task task1 = new Task();
        task1.setId(1);
        Task task2 = new Task();
        task2.setId(2);
        lesson.setTasks(List.of(task1, task2));

        TaskProgress taskProgress1 = new TaskProgress();
        taskProgress1.setTask(task1);
        taskProgress1.setCompleted(true);
        TaskProgress taskProgress2 = new TaskProgress();
        taskProgress2.setTask(task2);
        taskProgress2.setCompleted(false);

        LessonProgress lessonProgress = new LessonProgress();
        lessonProgress.setTaskProgresses(List.of(taskProgress1, taskProgress2));

        List<Lesson> lessons = List.of(lesson);
        Page<Lesson> lessonPage = new PageImpl<>(lessons);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
        when(lessonRepository.findAllByGroupsContaining(mockGroup, PageRequest.of(page, size)))
                .thenReturn(lessonPage);
        when(lessonProgressRepository.findByUserAndLesson(mockUser, lesson)).thenReturn(Optional.of(lessonProgress));

        Page<LessonsDisplayResponse> response = lessonService.getLessonsForDisplay(mockUser, groupId, page, size);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        LessonsDisplayResponse displayResponse = response.getContent().get(0);
        assertEquals(1, displayResponse.getLessonId());
        assertEquals("Lesson 1", displayResponse.getTitle());
        assertEquals(1, displayResponse.getCompletedTasks());
        assertEquals(2, displayResponse.getTotalTasks());
    }

    @Test
    void shouldReturnPaginatedLessonsForDisplay() {
        int page = 1;
        int size = 2;
        Lesson lesson1 = new Lesson();
        lesson1.setId(1);
        lesson1.setTitle("Lesson 1");
        Lesson lesson2 = new Lesson();
        lesson2.setId(2);
        lesson2.setTitle("Lesson 2");
        Lesson lesson3 = new Lesson();
        lesson3.setId(3);
        lesson3.setTitle("Lesson 3");

        List<Lesson> lessons = List.of(lesson1, lesson2, lesson3);
        Page<Lesson> lessonPage = new PageImpl<>(lessons.subList(2, 3), PageRequest.of(page, size), lessons.size());
        when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
        when(lessonRepository.findAllByGroupsContaining(mockGroup, PageRequest.of(page, size)))
                .thenReturn(lessonPage);

        Page<LessonsDisplayResponse> response = lessonService.getLessonsForDisplay(mockUser, 1, page, size);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Lesson 3", response.getContent().get(0).getTitle());
    }

    @Test
    void shouldReturnTasksForLessonWithCompletionStatus() {
        int lessonId = 1;
        User user = new User();
        user.setId(1);

        Task task1 = new Task();
        task1.setId(1);
        Task task2 = new Task();
        task2.setId(2);
        List<Task> tasks = List.of(task1, task2);

        LessonProgress lessonProgress = LessonProgress.builder()
                .id(1)
                .user(user)
                .lesson(new Lesson())
                .build();

        TaskProgress taskProgress1 = TaskProgress.builder()
                .lessonProgress(lessonProgress)
                .task(task1)
                .completed(true)
                .build();
        TaskProgress taskProgress2 = TaskProgress.builder()
                .lessonProgress(lessonProgress)
                .task(task2)
                .completed(false)
                .build();

        lessonProgress.setTaskProgresses(List.of(taskProgress1, taskProgress2));

        when(lessonProgressRepository.findByUserAndLessonId(user, lessonId)).thenReturn(lessonProgress);
        when(taskRepository.findByLessonId(lessonId)).thenReturn(tasks);

        List<TaskDisplayResponse> response = lessonService.getTasksForLesson(user, lessonId);

        assertNotNull(response);
        assertEquals(2, response.size());

        TaskDisplayResponse taskResponse1 = response.get(0);
        assertEquals(1, taskResponse1.getTaskId());
        assertTrue(taskResponse1.isCompleted());

        TaskDisplayResponse taskResponse2 = response.get(1);
        assertEquals(2, taskResponse2.getTaskId());
        assertFalse(taskResponse2.isCompleted());
    }

    @Test
    void shouldReturnLessonsWhenUserOwnsLessons() {
        User user = new User();
        user.setId(1);
        int page = 0;
        int size = 10;

        Lesson lesson1 = new Lesson();
        lesson1.setId(1);
        lesson1.setTitle("Lesson 1");
        Group group1 = new Group();
        group1.setId(1);
        lesson1.setGroups(List.of(group1));

        Lesson lesson2 = new Lesson();
        lesson2.setId(2);
        lesson2.setTitle("Lesson 2");
        Group group2 = new Group();
        group2.setId(2);
        lesson2.setGroups(List.of(group2));

        Page<Lesson> lessonPage = new PageImpl<>(List.of(lesson1, lesson2));

        when(lessonRepository.findLessonsByOwner(user, PageRequest.of(page, size))).thenReturn(lessonPage);

        Page<LessonWithGroupsResponse> response = lessonService.getLessonsOwnedByUser(user, page, size);

        assertNotNull(response);
        assertEquals(2, response.getTotalElements());
        assertEquals(2, response.getContent().size());

        LessonWithGroupsResponse firstLesson = response.getContent().get(0);
        assertEquals(1, firstLesson.getLessonId());
        assertEquals("Lesson 1", firstLesson.getTitle());
        assertTrue(firstLesson.getGroupIds().contains(1));

        LessonWithGroupsResponse secondLesson = response.getContent().get(1);
        assertEquals(2, secondLesson.getLessonId());
        assertEquals("Lesson 2", secondLesson.getTitle());
        assertTrue(secondLesson.getGroupIds().contains(2));
    }

    @Test
    void shouldReturnEmptyPageWhenUserHasNoLessonsOwnedByUser() {
        User user = new User();
        user.setId(1);
        int page = 0;
        int size = 10;

        when(lessonRepository.findLessonsByOwner(user, PageRequest.of(page, size))).thenReturn(Page.empty());

        Page<LessonWithGroupsResponse> response = lessonService.getLessonsOwnedByUser(user, page, size);

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void shouldUpdateLessonWhenUserIsOwner() {
        Integer lessonId = 1;
        User user = new User();
        user.setId(1);
        LessonUpdateRequest lessonUpdateRequest = new LessonUpdateRequest();
        lessonUpdateRequest.setTitle("Updated Lesson Title");
        lessonUpdateRequest.setGroupIds(List.of(1, 2));

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setTitle("Original Title");

        Group group1 = new Group();
        group1.setId(1);
        Group group2 = new Group();
        group2.setId(2);
        lesson.setGroups(List.of(group1, group2));

        UserGroup userGroup = new UserGroup();
        userGroup.setOwner(true);
        userGroup.setGroup(group1);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(userGroupRepository.findByUserAndGroup(user, group1)).thenReturn(Optional.of(userGroup));
        when(groupRepository.findAllById(lessonUpdateRequest.getGroupIds())).thenReturn(List.of(group1, group2));

        boolean result = lessonService.updateLesson(lessonId, lessonUpdateRequest, user);

        assertTrue(result);
        assertEquals("Updated Lesson Title", lesson.getTitle());
        assertEquals(2, lesson.getGroups().size());
    }

    @Test
    void shouldNotUpdateLessonWhenUserIsNotOwner() {
        Integer lessonId = 1;
        User user = new User();
        user.setId(1);
        LessonUpdateRequest lessonUpdateRequest = new LessonUpdateRequest();
        lessonUpdateRequest.setTitle("Updated Lesson Title");
        lessonUpdateRequest.setGroupIds(List.of(1));

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setTitle("Original Title");

        Group group1 = new Group();
        group1.setId(1);
        lesson.setGroups(List.of(group1));

        UserGroup userGroup = new UserGroup();
        userGroup.setOwner(false);
        userGroup.setGroup(group1);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(userGroupRepository.findByUserAndGroup(user, group1)).thenReturn(Optional.of(userGroup));

        boolean result = lessonService.updateLesson(lessonId, lessonUpdateRequest, user);

        assertFalse(result);
        assertEquals("Original Title", lesson.getTitle());
    }

    @Test
    void shouldNotUpdateLessonWhenLessonNotFound() {
        Integer lessonId = 1;
        User user = new User();
        user.setId(1);
        LessonUpdateRequest lessonUpdateRequest = new LessonUpdateRequest();
        lessonUpdateRequest.setTitle("Updated Lesson Title");
        lessonUpdateRequest.setGroupIds(List.of(1));

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        boolean result = lessonService.updateLesson(lessonId, lessonUpdateRequest, user);

        assertFalse(result);
    }

    @Test
    void shouldNotUpdateLessonWhenUserHasNoGroups() {
        Integer lessonId = 1;
        User user = new User();
        user.setId(1);
        LessonUpdateRequest lessonUpdateRequest = new LessonUpdateRequest();
        lessonUpdateRequest.setTitle("Updated Lesson Title");
        lessonUpdateRequest.setGroupIds(List.of(1));

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setTitle("Original Title");

        Group group1 = new Group();
        group1.setId(1);
        lesson.setGroups(List.of(group1));

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(userGroupRepository.findByUserAndGroup(user, group1)).thenReturn(Optional.empty());

        boolean result = lessonService.updateLesson(lessonId, lessonUpdateRequest, user);

        assertFalse(result);
        assertEquals("Original Title", lesson.getTitle());
    }

    @Test
    void shouldDeleteLessonWhenUserIsOwner() {
        Integer lessonId = 1;
        User user = new User();
        user.setId(1);

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setOwner(user);

        Group group = new Group();
        group.setId(1);
        group.setLessons(new ArrayList<>(List.of(lesson)));
        lesson.setGroups(new ArrayList<>(List.of(group)));

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        boolean result = lessonService.deleteLesson(lessonId, user);

        assertTrue(result);
        verify(lessonRepository, times(1)).delete(lesson);
        assertTrue(group.getLessons().isEmpty());
        assertTrue(lesson.getGroups().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenLessonNotFound() {
        Integer lessonId = 1;
        User user = new User();
        user.setId(1);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            lessonService.deleteLesson(lessonId, user);
        });
        assertEquals("Lesson not found", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotOwner() {
        Integer lessonId = 1;
        User user = new User();
        user.setId(1);

        User anotherUser = new User();
        anotherUser.setId(2);

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setOwner(anotherUser);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            lessonService.deleteLesson(lessonId, user);
        });
        assertEquals("You are not authorized to delete this lesson", exception.getMessage());
    }

    @Test
    void shouldDeleteLessonWithoutGroups() {
        Integer lessonId = 1;
        User user = new User();
        user.setId(1);

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setOwner(user);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        boolean result = lessonService.deleteLesson(lessonId, user);

        assertTrue(result);
        verify(lessonRepository, times(1)).delete(lesson);
    }
}

