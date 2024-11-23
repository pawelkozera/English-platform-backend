package com.learning.english.controllers;

import com.learning.english.dto.RepetitionAddRequest;
import com.learning.english.dto.RepetitionDisplayResponse;
import com.learning.english.models.User;
import com.learning.english.service.RepetitionService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @DeleteMapping("/remove/{wordId}")
    public ResponseEntity<String> removeRepetition(HttpServletRequest request, @PathVariable Integer wordId) {
        User user = authUtil.getAuthenticatedUser(request);
        repetitionService.removeRepetition(wordId, user);
        return ResponseEntity.ok("Repetition removed successfully");
    }

    @GetMapping("/exists/{wordId}")
    public ResponseEntity<Boolean> isWordInRepetitions(HttpServletRequest request, @PathVariable Integer wordId) {
        User user = authUtil.getAuthenticatedUser(request);
        boolean exists = repetitionService.isWordInRepetitions(wordId, user);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/count/today/{groupId}")
    public ResponseEntity<Long> getRepetitionsForTodayByGroup(HttpServletRequest request, @PathVariable Integer groupId) {
        User user = authUtil.getAuthenticatedUser(request);
        long count = repetitionService.countRepetitionsForTodayByGroup(user, groupId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/words/{groupId}")
    public ResponseEntity<List<RepetitionDisplayResponse>> getRepetitionWords(
            HttpServletRequest request,
            @PathVariable Integer groupId,
            @RequestParam(defaultValue = "30") int limit) {
        User user = authUtil.getAuthenticatedUser(request);
        List<RepetitionDisplayResponse> words = repetitionService.getRepetitionWords(user, groupId, limit);
        return ResponseEntity.ok(words);
    }
}
