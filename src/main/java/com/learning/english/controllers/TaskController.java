package com.learning.english.controllers;

import com.learning.english.dto.LessonAddRequest;
import com.learning.english.dto.TaskAddRequest;
import com.learning.english.dto.TaskCompleteRequest;
import com.learning.english.dto.TaskResponse;
import com.learning.english.models.User;
import com.learning.english.service.TaskService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/task")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final AuthUtil authUtil;

    @PostMapping("/add")
    public ResponseEntity<String> createTask(HttpServletRequest request, @RequestBody TaskAddRequest taskAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        taskService.addTask(taskAddRequest, user);
        return ResponseEntity.ok("Task created successfully");
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTaskById(HttpServletRequest request, @PathVariable Integer taskId) {
        User user = authUtil.getAuthenticatedUser(request);
        TaskResponse taskResponse = taskService.getTaskById(user, taskId);
        return ResponseEntity.ok(taskResponse);
    }

    @GetMapping("/batch")
    public ResponseEntity<List<TaskResponse>> getTasksByIds(HttpServletRequest request, @RequestParam List<Integer> taskIds) {
        User user = authUtil.getAuthenticatedUser(request);
        List<TaskResponse> tasks = taskService.getTasksByIds(user, taskIds);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/complete")
    public ResponseEntity<Void> completeTask(HttpServletRequest request, @RequestBody TaskCompleteRequest taskCompleteRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        taskService.completeTask(user, taskCompleteRequest);
        return ResponseEntity.ok().build();
    }
}

