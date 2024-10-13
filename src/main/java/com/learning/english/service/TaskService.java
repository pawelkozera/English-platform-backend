package com.learning.english.service;

import com.learning.english.dto.TaskAddRequest;
import com.learning.english.dto.TaskResponse;
import com.learning.english.dto.WordResponse;
import com.learning.english.exception.TaskNotFoundException;
import com.learning.english.models.*;
import com.learning.english.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final TaskSubTypeRepository taskSubTypeRepository;
    private final LessonRepository lessonRepository;
    private final UserGroupRepository userGroupRepository;
    private final WordRepository wordRepository;
    private final GroupRepository groupRepository;

    public void addTask(TaskAddRequest taskAddRequest, User user) {
        TaskType taskType = taskTypeRepository.findByTypeName(taskAddRequest.getTaskTypeName())
                .orElseThrow(() -> new IllegalArgumentException("Invalid task type"));

        TaskSubType taskSubType = taskSubTypeRepository.findBySubTypeName(taskAddRequest.getTaskSubTypeName())
                .orElseThrow(() -> new IllegalArgumentException("Invalid task sub type"));

        Lesson lesson = lessonRepository.findById(taskAddRequest.getLessonId())
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        boolean isOwner = lesson.getGroups().stream().anyMatch(group ->
                userGroupRepository.existsByUserAndGroupAndIsOwnerTrue(user, group));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not the owner of any group associated with the lesson");
        }

        List<Word> words = StreamSupport.stream(wordRepository.findAllById(taskAddRequest.getWordIds()).spliterator(), false)
                .collect(Collectors.toList());

        Task task = Task.builder()
                .taskType(taskType)
                .taskSubType(taskSubType)
                .content(taskAddRequest.getContent())
                .correctAnswer(taskAddRequest.getCorrectAnswer())
                .lesson(lesson)
                .words(words)
                .build();

        taskRepository.save(task);
    }

    public TaskResponse getTaskById(User user, Integer taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException("Task not found"));

        Lesson lesson = task.getLesson();
        boolean userInGroup = lesson.getGroups().stream()
                .anyMatch(group -> groupRepository.existsByGroupIdAndUserId(group.getId(), user.getId()));

        if (!userInGroup) {
            throw new AccessDeniedException("User does not belong to any group for this task's lesson");
        }

        return TaskResponse.builder()
                .id(task.getId())
                .taskTypeName(task.getTaskType().getTypeName())
                .taskSubTypeName(task.getTaskSubType().getSubTypeName())
                .content(task.getContent())
                .correctAnswer(task.getCorrectAnswer())
                .words(task.getWords().stream()
                        .map(word -> WordResponse.builder()
                                .id(word.getId())
                                .word(word.getWord())
                                .translation(word.getTranslation())
                                .audioFilePath(word.getAudioFilePath())
                                .imageFilePath(word.getImageFilePath())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}