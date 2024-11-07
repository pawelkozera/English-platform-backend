package com.learning.english.controllers;

import com.learning.english.dto.SuspiciousActivityAddRequest;
import com.learning.english.models.User;
import com.learning.english.service.SuspiciousActivityService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suspicious/activity")
@RequiredArgsConstructor
public class SuspiciousActivityController {
    private final AuthUtil authUtil;
    private final SuspiciousActivityService suspiciousActivityService;

    @PostMapping("/add")
    public ResponseEntity<String> createSuspiciousActivity(HttpServletRequest request, @RequestBody SuspiciousActivityAddRequest suspiciousActivityAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        return suspiciousActivityService.addSuspiciousActivity(suspiciousActivityAddRequest, user);
    }
}
