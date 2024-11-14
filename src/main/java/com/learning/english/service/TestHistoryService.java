package com.learning.english.service;

import com.learning.english.dto.TestHistoryAddRequest;
import com.learning.english.models.TestHistory;
import com.learning.english.models.TestInstance;
import com.learning.english.models.User;
import com.learning.english.repository.TestHistoryRepository;
import com.learning.english.repository.TestInstanceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TestHistoryService {
    private final TestHistoryRepository testHistoryRepository;
    private final TestInstanceRepository testInstanceRepository;

    @Transactional
    public ResponseEntity<String> addTestHistory(TestHistoryAddRequest testHistoryAddRequest, User user) {
        Integer testInstanceId = testHistoryAddRequest.getTestInstanceId();

        Optional<TestInstance> testInstanceOpt = testInstanceRepository.findById(testInstanceId);
        if (testInstanceOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Test instance not found");
        }
        TestInstance testInstance = testInstanceOpt.get();

        Optional<TestHistory> existingTestHistoryOpt = testHistoryRepository.findByTestInstanceAndUser(testInstance, user);

        if (existingTestHistoryOpt.isEmpty()) {
            if (testInstance.getEndTime().isBefore(LocalDateTime.now()) || testInstance.getActivationTime().isAfter(LocalDateTime.now())) {
                return ResponseEntity.status(403).body("Access denied: Test has already ended or hasn't started yet");
            }

            TestHistory testHistory = createNewTestHistory(testInstance, user);
            testHistoryRepository.save(testHistory);
            return ResponseEntity.ok("Test started successfully");
        }

        int score = testHistoryAddRequest.getScore();
        if (score == -1) {
            return ResponseEntity.status(403).body("Access denied: Test already completed");
        }

        TestHistory testHistory = existingTestHistoryOpt.get();

        if (testHistory.getCompletedAt() == null) {
            testHistory.setScore(score);
            testHistory.setCompletedAt(LocalDateTime.now());
            testHistoryRepository.save(testHistory);
            return ResponseEntity.ok("Test progress saved successfully");
        }

        return ResponseEntity.badRequest().body("Test already completed");
    }

    private TestHistory createNewTestHistory(TestInstance testInstance, User user) {
        TestHistory testHistory = new TestHistory();
        testHistory.setTestInstance(testInstance);
        testHistory.setUser(user);
        testHistory.setScore(0);
        testHistory.setSuspiciousActivityDetected(false);
        return testHistory;
    }

    public ResponseEntity<String> checkTestCompletion(Integer testInstanceId, User user) {
        Optional<TestInstance> testInstanceOptional = testInstanceRepository.findById(testInstanceId);

        if (testInstanceOptional.isEmpty()) {
            return ResponseEntity.status(404).body("Test instance not found");
        }

        TestInstance testInstance = testInstanceOptional.get();

        Optional<TestHistory> testHistoryOptional = testHistoryRepository.findByTestInstanceAndUser(testInstance, user);

        if (testHistoryOptional.isEmpty()) {
            return ResponseEntity.status(404).body("Test history not found for user");
        }

        TestHistory testHistory = testHistoryOptional.get();

        if (testHistory.getCompletedAt() != null && testHistory.getCompletedAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(403).body("Test has already ended");
        } else {
            return ResponseEntity.ok("Test is still active");
        }
    }

}
