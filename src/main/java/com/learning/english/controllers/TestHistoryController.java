package com.learning.english.controllers;

import com.learning.english.dto.TestHistoryAddRequest;
import com.learning.english.models.User;
import com.learning.english.service.TestHistoryService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test/history")
@RequiredArgsConstructor
public class TestHistoryController {
    private final AuthUtil authUtil;
    private final TestHistoryService testHistoryService;

    @PostMapping("/add")
    public ResponseEntity<String> createTestHistory(HttpServletRequest request, @RequestBody TestHistoryAddRequest testHistoryAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        return testHistoryService.addTestHistory(testHistoryAddRequest, user);
    }

    @GetMapping("/check/completion/{testInstanceId}")
    public ResponseEntity<String> checkTestCompletion(HttpServletRequest request, @PathVariable Integer testInstanceId) {
        User user = authUtil.getAuthenticatedUser(request);
        return testHistoryService.checkTestCompletion(testInstanceId, user);
    }

}