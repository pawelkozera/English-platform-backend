package com.learning.english.controllers;

import com.learning.english.dto.LessonAddRequest;
import com.learning.english.dto.TaskAddRequest;
import com.learning.english.models.User;
import com.learning.english.service.JwtService;
import com.learning.english.service.LessonService;
import com.learning.english.service.UserService;
import com.learning.english.utils.TokenCookies;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lesson")
@RequiredArgsConstructor
public class LessonController {
    private final JwtService jwtService;
    private final UserService userService;
    private final LessonService lessonService;

    @PostMapping("/add")
    public ResponseEntity<String> addLesson(HttpServletRequest request, @RequestBody LessonAddRequest lessonAddRequest) {
        String jwtToken = TokenCookies.extractAccessToken(request);
        String email = jwtService.extractUserName(jwtToken);

        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        lessonService.addLesson(lessonAddRequest, user);

        return ResponseEntity.ok("Task created successfully");
    }
}
