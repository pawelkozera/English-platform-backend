package com.learning.english.controllers;

import com.learning.english.dto.TaskAddRequest;
import com.learning.english.models.User;
import com.learning.english.service.JwtService;
import com.learning.english.service.TaskService;
import com.learning.english.service.UserService;
import com.learning.english.utils.TokenCookies;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/task")
@RequiredArgsConstructor
public class TaskController {
    private final JwtService jwtService;
    private final UserService userService;
    private final TaskService taskService;

    @PostMapping("/add")
    public ResponseEntity<String> createTask(HttpServletRequest request, @RequestBody TaskAddRequest taskAddRequest) {

        String jwtToken = TokenCookies.extractAccessToken(request);
        String email = jwtService.extractUserName(jwtToken);

        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        taskService.addTask(taskAddRequest, user);

        return ResponseEntity.ok("Task created successfully");
    }
}

