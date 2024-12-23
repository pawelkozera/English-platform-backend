package com.learning.english.unit;

import com.learning.english.dto.SuspiciousActivityAddRequest;
import com.learning.english.models.SuspiciousActivity;
import com.learning.english.models.TestHistory;
import com.learning.english.models.User;
import com.learning.english.repository.SuspiciousActivityRepository;
import com.learning.english.repository.TestHistoryRepository;
import com.learning.english.service.SuspiciousActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SuspiciousActivityServiceTest {

    @Mock
    private SuspiciousActivityRepository suspiciousActivityRepository;

    @Mock
    private TestHistoryRepository testHistoryRepository;

    @InjectMocks
    private SuspiciousActivityService suspiciousActivityService;

    private User user;
    private TestHistory testHistory;
    private SuspiciousActivityAddRequest suspiciousActivityAddRequest;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Set up test data
        user = User.builder().id(1).firstName("John").lastName("Doe").email("john.doe@example.com").build();
        testHistory = TestHistory.builder().id(1).user(user).suspiciousActivityDetected(false).build();
        suspiciousActivityAddRequest = SuspiciousActivityAddRequest.builder()
                .testInstanceId(1)
                .description("Tab Switching")
                .build();
    }

    @Test
    public void testAddSuspiciousActivity_NewActivity() {
        when(testHistoryRepository.findByTestInstanceIdAndUserId(anyInt(), anyInt()))
                .thenReturn(Optional.of(testHistory));
        when(suspiciousActivityRepository.findByTestHistoryAndDescription(any(), any()))
                .thenReturn(null);

        ResponseEntity<String> response = suspiciousActivityService.addSuspiciousActivity(suspiciousActivityAddRequest, user);

        verify(suspiciousActivityRepository, times(1)).save(any(SuspiciousActivity.class));
        verify(testHistoryRepository, times(1)).save(testHistory);
        assertEquals("Suspicious activity recorded successfully", response.getBody());
        assertTrue(testHistory.isSuspiciousActivityDetected());
    }

    @Test
    public void testAddSuspiciousActivity_ExistingActivity() {
        SuspiciousActivity existingActivity = SuspiciousActivity.builder()
                .testHistory(testHistory)
                .description("Tab Switching")
                .occurrenceCount(1)
                .build();

        when(testHistoryRepository.findByTestInstanceIdAndUserId(anyInt(), anyInt()))
                .thenReturn(Optional.of(testHistory));
        when(suspiciousActivityRepository.findByTestHistoryAndDescription(testHistory, "Tab Switching"))
                .thenReturn(existingActivity);

        ResponseEntity<String> response = suspiciousActivityService.addSuspiciousActivity(suspiciousActivityAddRequest, user);

        verify(suspiciousActivityRepository, times(1)).save(existingActivity);
        assertEquals(2, existingActivity.getOccurrenceCount());
        assertEquals("Suspicious activity recorded successfully", response.getBody());
    }

    @Test
    public void testAddSuspiciousActivity_NoTestHistoryFound() {
        when(testHistoryRepository.findByTestInstanceIdAndUserId(anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                suspiciousActivityService.addSuspiciousActivity(suspiciousActivityAddRequest, user)
        );
        assertEquals("No TestHistory found for the given TestInstance and User", exception.getMessage());
    }
}