package com.learning.english.controllers;

import com.learning.english.dto.WordAddRequest;
import com.learning.english.models.User;
import com.learning.english.service.WordService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/word")
@RequiredArgsConstructor
public class WordController {
    private final WordService wordService;
    private final AuthUtil authUtil;

    @PostMapping("/add")
    public ResponseEntity<String> addWord(HttpServletRequest request, @RequestBody WordAddRequest wordAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        wordService.addTask(wordAddRequest, user);
        return ResponseEntity.ok("Word created successfully");
    }
}
