package com.learning.english.controllers;

import com.learning.english.dto.RepetitionAddRequest;
import com.learning.english.models.User;
import com.learning.english.service.RepetitionService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/repetition")
@RequiredArgsConstructor
public class RepetitionController {
    private final AuthUtil authUtil;
    private final RepetitionService repetitionService;

    @PostMapping("/add")
    public ResponseEntity<String> addRepetition(HttpServletRequest request, @RequestBody RepetitionAddRequest repetitionAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        repetitionService.addRepetition(repetitionAddRequest, user);
        return ResponseEntity.ok("Repetition added successfully");
    }
}
