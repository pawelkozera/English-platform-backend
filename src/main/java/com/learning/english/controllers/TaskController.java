package com.learning.english.controllers;

import com.learning.english.dto.TaskAddRequest;
import com.learning.english.dto.TaskResponse;
import com.learning.english.models.User;
import com.learning.english.service.TaskService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

