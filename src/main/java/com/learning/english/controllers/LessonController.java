package com.learning.english.controllers;

import com.learning.english.dto.LessonAddRequest;
import com.learning.english.dto.LessonResponse;
import com.learning.english.models.User;
import com.learning.english.service.LessonService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lesson")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;
    private final AuthUtil authUtil;

    @PostMapping("/add")
    public ResponseEntity<String> addLesson(HttpServletRequest request, @RequestBody LessonAddRequest lessonAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        lessonService.addLesson(lessonAddRequest, user);
        return ResponseEntity.ok("Task created successfully");
    }

    @GetMapping("all/from/group/{groupId}")
    public List<LessonResponse> getLessonsFromGroup(HttpServletRequest request, @PathVariable Integer groupId) {
        User user = authUtil.getAuthenticatedUser(request);
        return lessonService.getLessonsFromGroup(user, groupId);
    }
}
