package com.learning.english.controllers;

import com.learning.english.dto.TestInstanceAddRequest;
import com.learning.english.models.User;
import com.learning.english.service.TestInstanceService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test/instance")
@RequiredArgsConstructor
public class TestInstanceController {
    private final AuthUtil authUtil;
    private final TestInstanceService testInstanceService;

    @PostMapping("/add")
    public ResponseEntity<String> launchTestInstance(HttpServletRequest request, @RequestBody TestInstanceAddRequest testInstanceAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        testInstanceService.addTestInstance(testInstanceAddRequest, user);
        return ResponseEntity.ok("Test instance created successfully");
    }
}
