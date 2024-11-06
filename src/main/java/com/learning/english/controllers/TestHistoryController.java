package com.learning.english.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        System.out.println("dotarło");
        User user = authUtil.getAuthenticatedUser(request);
        return testHistoryService.addTestHistory(testHistoryAddRequest, user);
    }

    @PostMapping("/addTestHistoryBeacon")
    public ResponseEntity<String> createTestHistoryViaBeacon(HttpServletRequest request, @RequestBody String payload) {
        System.out.println("Dotarlo beacon");

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            TestHistoryAddRequest testHistoryAddRequest = objectMapper.readValue(payload, TestHistoryAddRequest.class);

            User user = authUtil.getAuthenticatedUser(request);
            return testHistoryService.addTestHistory(testHistoryAddRequest, user);

        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body("Invalid JSON format");
        }
    }

    @GetMapping("/check/completion/{testInstanceId}")
    public ResponseEntity<String> checkTestCompletion(HttpServletRequest request, @PathVariable Integer testInstanceId) {
        User user = authUtil.getAuthenticatedUser(request);
        return testHistoryService.checkTestCompletion(testInstanceId, user);
    }

}