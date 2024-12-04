package com.learning.english.service;

import com.learning.english.dto.*;
import com.learning.english.models.*;
import com.learning.english.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final TaskRepository taskRepository;
    private final TaskProgressRepository taskProgressRepository;

    public void addLesson(LessonAddRequest lessonAddRequest, User user) {
        List<Group> groups = groupRepository.findAllById(lessonAddRequest.getGroupId());
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("No groups found for the provided IDs");
        }

        boolean isOwnerForAllGroups = groups.stream()
                .allMatch(group -> userGroupRepository.existsByUserAndGroupAndIsOwnerTrue(user, group));
        if (!isOwnerForAllGroups) {
            throw new IllegalArgumentException("User is not the owner of one or more selected groups");
        }

        boolean lessonExists = groups.stream()
                .flatMap(group -> group.getLessons().stream())
                .anyMatch(existingLesson -> existingLesson.getTitle().equalsIgnoreCase(lessonAddRequest.getTitle()));
        if (lessonExists) {
            throw new IllegalArgumentException("A lesson with this title already exists in one of the groups");
        }

        Lesson lesson = Lesson.builder()
                .title(lessonAddRequest.getTitle())
                .build();

        groups.forEach(group -> {
            group.getLessons().add(lesson);
            lesson.getGroups().add(group);
        });

        lessonRepository.save(lesson);
    }

    public Page<LessonResponse> getLessonsFromGroup(User user, Integer groupId, int page, int size) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        boolean isMember = userGroupRepository.existsByUserAndGroup(user, group);
        if (!isMember) {
            throw new IllegalArgumentException("User is not a member of the group");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Lesson> lessonsPage = lessonRepository.findAllByGroupsContaining(group, pageable);

        return lessonsPage.map(lesson -> LessonResponse.builder()
                .lessonId(lesson.getId())
                .title(lesson.getTitle())
                .build());
    }

    public Page<LessonResponse> getLessonsNotAssignedToGroup(User user, Integer groupId, int page, int size) {
        Optional<Group> groupOptional = groupRepository.findById(groupId);
        if (groupOptional.isEmpty()) {
            throw new EntityNotFoundException("Group not found");
        }

        Group group = groupOptional.get();

        Optional<UserGroup> userGroupOptional = userGroupRepository.findByUserAndGroup(user, group);
        if (userGroupOptional.isEmpty() || !userGroupOptional.get().isOwner()) {
            throw new IllegalArgumentException("User is not the owner of the group.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Lesson> lessons = lessonRepository.findLessonsNotAssignedToGroup(groupId, pageable);

        return lessons.map(lesson -> LessonResponse.builder()
                .lessonId(lesson.getId())
                .title(lesson.getTitle())
                .build());
    }

    public Page<LessonsDisplayResponse> getLessonsForDisplay(User user, Integer groupId, int page, int size) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Lesson> lessons = lessonRepository.findAllByGroupsContaining(group, pageable);

        return lessons.map(lesson -> {
            LessonProgress lessonProgress = lessonProgressRepository.findByUserAndLesson(user, lesson).orElse(null);

            int completedTasks = lessonProgress == null ? 0 :
                    (int) lessonProgress.getTaskProgresses().stream()
                            .filter(TaskProgress::isCompleted)
                            .count();

            int totalTasks = lesson.getTasks().size();

            return LessonsDisplayResponse.builder()
                    .lessonId(lesson.getId())
                    .title(lesson.getTitle())
                    .completedTasks(completedTasks)
                    .totalTasks(totalTasks)
                    .build();
        });
    }

    public List<TaskDisplayResponse> getTasksForLesson(User user, Integer lessonId) {
        LessonProgress lessonProgress = lessonProgressRepository.findByUserAndLessonId(user, lessonId);
        List<Task> tasks = taskRepository.findByLessonId(lessonId);

        if (lessonProgress == null) {
            lessonProgress = LessonProgress.builder()
                    .user(user)
                    .lesson(lessonRepository.findById(lessonId)
                            .orElseThrow(() -> new EntityNotFoundException("Lesson not found")))
                    .build();

            lessonProgress = lessonProgressRepository.save(lessonProgress);

            LessonProgress finalLessonProgress2 = lessonProgress;
            List<TaskProgress> newTaskProgresses = tasks.stream()
                    .map(task -> TaskProgress.builder()
                            .lessonProgress(finalLessonProgress2)
                            .task(task)
                            .completed(false)
                            .build())
                    .toList();
            taskProgressRepository.saveAll(newTaskProgresses);

            lessonProgress = lessonProgressRepository.findById(lessonProgress.getId())
                    .orElseThrow(() -> new EntityNotFoundException("LessonProgress not found after saving"));
        } else {
            List<TaskProgress> existingTaskProgresses = taskProgressRepository.findByLessonProgress(lessonProgress);

            Set<Integer> existingTaskIds = existingTaskProgresses.stream()
                    .map(taskProgress -> taskProgress.getTask().getId())
                    .collect(Collectors.toSet());

            LessonProgress finalLessonProgress1 = lessonProgress;
            List<TaskProgress> newTaskProgresses = tasks.stream()
                    .filter(task -> !existingTaskIds.contains(task.getId()))
                    .map(task -> TaskProgress.builder()
                            .lessonProgress(finalLessonProgress1)
                            .task(task)
                            .completed(false)
                            .build())
                    .toList();

            if (!newTaskProgresses.isEmpty()) {
                taskProgressRepository.saveAll(newTaskProgresses);
            }
        }

        final LessonProgress finalLessonProgress = lessonProgress;
        return tasks.stream().map(task -> {
            boolean completed = finalLessonProgress.getTaskProgresses() != null && finalLessonProgress.getTaskProgresses().stream()
                    .anyMatch(taskProgress -> taskProgress.getTask().getId().equals(task.getId()) && taskProgress.isCompleted());

            return new TaskDisplayResponse(task.getId(), completed);
        }).collect(Collectors.toList());
    }
}
