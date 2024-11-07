package com.learning.english.service;

import com.learning.english.dto.SuspiciousActivityAddRequest;
import com.learning.english.models.SuspiciousActivity;
import com.learning.english.models.TestHistory;
import com.learning.english.models.User;
import com.learning.english.repository.SuspiciousActivityRepository;
import com.learning.english.repository.TestHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SuspiciousActivityService {

    private final SuspiciousActivityRepository suspiciousActivityRepository;
    private final TestHistoryRepository testHistoryRepository;

    public ResponseEntity<String> addSuspiciousActivity(SuspiciousActivityAddRequest request, User user) {
        TestHistory testHistory = testHistoryRepository
                .findByTestInstanceIdAndUserId(request.getTestInstanceId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No TestHistory found for the given TestInstance and User"));

        SuspiciousActivity suspiciousActivity = SuspiciousActivity.builder()
                .testHistory(testHistory)
                .timestamp(LocalDateTime.now())
                .description(request.getDescription())
                .build();

        suspiciousActivityRepository.save(suspiciousActivity);

        if (!testHistory.isSuspiciousActivityDetected()) {
            testHistory.setSuspiciousActivityDetected(true);
            testHistoryRepository.save(testHistory);
        }

        return ResponseEntity.ok("Suspicious activity recorded successfully");
    }
}
