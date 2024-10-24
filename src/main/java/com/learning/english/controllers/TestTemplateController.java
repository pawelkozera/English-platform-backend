package com.learning.english.controllers;

import com.learning.english.dto.TemplateAddRequest;
import com.learning.english.models.User;
import com.learning.english.service.TestTemplateService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test/template")
@RequiredArgsConstructor
public class TestTemplateController {
    private final AuthUtil authUtil;
    private final TestTemplateService testTemplateService;

    @PostMapping("/add")
    public ResponseEntity<String> createTemplate(HttpServletRequest request, @RequestBody TemplateAddRequest templateAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        testTemplateService.addTemplate(templateAddRequest, user);
        return ResponseEntity.ok("Template created successfully");
    }
}
