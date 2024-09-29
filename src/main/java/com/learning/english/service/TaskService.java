package com.learning.english.service;

import com.learning.english.dto.TaskAddRequest;
import com.learning.english.models.*;
import com.learning.english.repository.*;
import lombok.RequiredArgsConstructor;
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
}