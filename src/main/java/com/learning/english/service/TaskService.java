package com.learning.english.service;

import com.learning.english.dto.TaskAddRequest;
import com.learning.english.models.Lesson;
import com.learning.english.models.Task;
import com.learning.english.models.TaskType;
import com.learning.english.models.User;
import com.learning.english.repository.LessonRepository;
import com.learning.english.repository.TaskRepository;
import com.learning.english.repository.TaskTypeRepository;
import com.learning.english.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final LessonRepository lessonRepository;
    private final UserGroupRepository userGroupRepository;

    public void addTask(TaskAddRequest taskAddRequest, User user) {
        TaskType taskType = taskTypeRepository.findByTypeName(taskAddRequest.getTaskTypeName())
                .orElseThrow(() -> new IllegalArgumentException("Invalid task type"));

        Lesson lesson = lessonRepository.findById(taskAddRequest.getLessonId())
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        boolean isOwner = lesson.getGroups().stream().anyMatch(group ->
                userGroupRepository.existsByUserAndGroupAndIsOwnerTrue(user, group));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not the owner of any group associated with the lesson");
        }

        Task task = Task.builder()
                .taskType(taskType)
                .content(taskAddRequest.getContent())
                .correctAnswer(taskAddRequest.getCorrectAnswer())
                .lesson(lesson)
                .build();

        taskRepository.save(task);
    }
}
