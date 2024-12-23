package com.learning.english.unit;

import com.learning.english.dto.TestHistoryAddRequest;
import com.learning.english.dto.TestHistoryDisplayResponse;
import com.learning.english.dto.TestResultDto;
import com.learning.english.models.*;
import com.learning.english.repository.TestHistoryRepository;
import com.learning.english.repository.TestInstanceRepository;
import com.learning.english.repository.UserGroupRepository;
import com.learning.english.service.TestHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TestHistoryServiceTest {

    @Mock
    private TestHistoryRepository testHistoryRepository;

    @Mock
    private TestInstanceRepository testInstanceRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @InjectMocks
    private TestHistoryService testHistoryService;

    private User user;
    private TestInstance testInstance;
    private TestHistoryAddRequest testHistoryAddRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);

        testInstance = new TestInstance();
        testInstance.setId(1);
        testInstance.setActivationTime(LocalDateTime.now().minusHours(1));
        testInstance.setEndTime(LocalDateTime.now().plusHours(1));

        testHistoryAddRequest = new TestHistoryAddRequest();
        testHistoryAddRequest.setTestInstanceId(1);
        testHistoryAddRequest.setScore(50);
    }

    @Test
    void shouldReturnBadRequestWhenTestInstanceNotFound() {
        when(testInstanceRepository.findById(1)).thenReturn(Optional.empty());

        ResponseEntity<String> response = testHistoryService.addTestHistory(testHistoryAddRequest, user);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Test instance not found", response.getBody());
    }

    @Test
    void shouldReturnForbiddenWhenTestIsAlreadyEndedOrNotStartedYet() {
        testInstance.setEndTime(LocalDateTime.now().minusHours(2));
        when(testInstanceRepository.findById(1)).thenReturn(Optional.of(testInstance));
        when(testHistoryRepository.findByTestInstanceAndUser(testInstance, user)).thenReturn(Optional.empty());

        ResponseEntity<String> response = testHistoryService.addTestHistory(testHistoryAddRequest, user);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Access denied: Test has already ended or hasn't started yet", response.getBody());
    }

    @Test
    void shouldReturnForbiddenWhenTestAlreadyCompleted() {
        testInstance.setEndTime(LocalDateTime.now().plusHours(1));
        when(testInstanceRepository.findById(1)).thenReturn(Optional.of(testInstance));

        TestHistory existingTestHistory = new TestHistory();
        existingTestHistory.setCompletedAt(LocalDateTime.now());
        when(testHistoryRepository.findByTestInstanceAndUser(testInstance, user)).thenReturn(Optional.of(existingTestHistory));

        testHistoryAddRequest.setScore(-1);
        ResponseEntity<String> response = testHistoryService.addTestHistory(testHistoryAddRequest, user);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Access denied: Test already completed", response.getBody());
    }


    @Test
    void shouldSaveTestProgressWhenTestIsInProgress() {
        testInstance.setEndTime(LocalDateTime.now().plusHours(1));
        when(testInstanceRepository.findById(1)).thenReturn(Optional.of(testInstance));
        when(testHistoryRepository.findByTestInstanceAndUser(testInstance, user)).thenReturn(Optional.empty());

        ResponseEntity<String> response = testHistoryService.addTestHistory(testHistoryAddRequest, user);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Test started successfully", response.getBody());
        verify(testHistoryRepository, times(1)).save(any(TestHistory.class));
    }

    @Test
    void shouldUpdateTestScoreWhenTestProgressIsInProgress() {
        TestHistory existingTestHistory = new TestHistory();
        existingTestHistory.setCompletedAt(null);
        existingTestHistory.setScore(0);
        when(testInstanceRepository.findById(1)).thenReturn(Optional.of(testInstance));
        when(testHistoryRepository.findByTestInstanceAndUser(testInstance, user)).thenReturn(Optional.of(existingTestHistory));

        ResponseEntity<String> response = testHistoryService.addTestHistory(testHistoryAddRequest, user);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Test progress saved successfully", response.getBody());
        assertEquals(50, existingTestHistory.getScore());
        assertNotNull(existingTestHistory.getCompletedAt());
        verify(testHistoryRepository, times(1)).save(existingTestHistory);
    }

    @Test
    void shouldReturnBadRequestWhenTestAlreadyCompleted() {
        TestHistory existingTestHistory = new TestHistory();
        existingTestHistory.setCompletedAt(LocalDateTime.now());
        existingTestHistory.setScore(100);
        when(testInstanceRepository.findById(1)).thenReturn(Optional.of(testInstance));
        when(testHistoryRepository.findByTestInstanceAndUser(testInstance, user)).thenReturn(Optional.of(existingTestHistory));

        ResponseEntity<String> response = testHistoryService.addTestHistory(testHistoryAddRequest, user);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Test already completed", response.getBody());
    }

    @Test
    void shouldReturnNotFoundWhenTestInstanceDoesNotExist() {
        Integer testInstanceId = 1;
        User user = new User();
        user.setId(1);

        when(testInstanceRepository.findById(testInstanceId)).thenReturn(Optional.empty());

        ResponseEntity<String> response = testHistoryService.checkTestCompletion(testInstanceId, user);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Test instance not found", response.getBody());
    }

    @Test
    void shouldReturnNotFoundWhenTestHistoryDoesNotExist() {
        Integer testInstanceId = 1;
        User user = new User();
        user.setId(1);

        TestInstance testInstance = new TestInstance();
        testInstance.setId(testInstanceId);

        when(testInstanceRepository.findById(testInstanceId)).thenReturn(Optional.of(testInstance));
        when(testHistoryRepository.findByTestInstanceAndUser(testInstance, user)).thenReturn(Optional.empty());

        ResponseEntity<String> response = testHistoryService.checkTestCompletion(testInstanceId, user);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Test history not found for user", response.getBody());
    }

    @Test
    void shouldReturnForbiddenWhenTestHasAlreadyEnded() {
        Integer testInstanceId = 1;
        User user = new User();
        user.setId(1);

        TestInstance testInstance = new TestInstance();
        testInstance.setId(testInstanceId);

        TestHistory testHistory = new TestHistory();
        testHistory.setCompletedAt(LocalDateTime.now().minusHours(1));

        when(testInstanceRepository.findById(testInstanceId)).thenReturn(Optional.of(testInstance));
        when(testHistoryRepository.findByTestInstanceAndUser(testInstance, user)).thenReturn(Optional.of(testHistory));

        ResponseEntity<String> response = testHistoryService.checkTestCompletion(testInstanceId, user);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Test has already ended", response.getBody());
    }

    @Test
    void shouldReturnOkWhenTestIsStillActive() {
        Integer testInstanceId = 1;
        User user = new User();
        user.setId(1);

        TestInstance testInstance = new TestInstance();
        testInstance.setId(testInstanceId);

        TestHistory testHistory = new TestHistory();
        testHistory.setCompletedAt(null);

        when(testInstanceRepository.findById(testInstanceId)).thenReturn(Optional.of(testInstance));
        when(testHistoryRepository.findByTestInstanceAndUser(testInstance, user)).thenReturn(Optional.of(testHistory));

        ResponseEntity<String> response = testHistoryService.checkTestCompletion(testInstanceId, user);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Test is still active", response.getBody());
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenUserDoesNotHaveAccessToGroup() {
        Integer groupId = 1;
        User user = new User();
        user.setId(1);

        when(userGroupRepository.findByUserAndGroupId(user, groupId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> {
            testHistoryService.getTestHistoryForDisplay(user, groupId, 0, 10);
        });
    }

    @Test
    void shouldReturnTestHistoryForDisplayWhenUserHasAccessAndHistoryExists() {
        Integer groupId = 1;
        User user = new User();
        user.setId(1);

        TestInstance testInstance = new TestInstance();
        TestTemplate testTemplate = new TestTemplate();
        testTemplate.setName("Test 1");
        testTemplate.setTotalScore(100);

        testInstance.setTestTemplate(testTemplate);
        testInstance.setActivationTime(LocalDateTime.now().minusDays(1));
        testInstance.setEndTime(LocalDateTime.now());
        testInstance.setTimeDuration(60);

        TestHistory testHistory = new TestHistory();
        testHistory.setTestInstance(testInstance);
        testHistory.setScore(80);

        UserGroup userGroup = new UserGroup();
        when(userGroupRepository.findByUserAndGroupId(user, groupId)).thenReturn(Optional.of(userGroup));

        Pageable pageable = PageRequest.of(0, 10);
        Page<TestHistory> testHistoryPage = new PageImpl<>(List.of(testHistory), pageable, 1);
        when(testHistoryRepository.findByUserAndTestInstanceGroupId(user, groupId, pageable)).thenReturn(testHistoryPage);

        Page<TestHistoryDisplayResponse> result = testHistoryService.getTestHistoryForDisplay(user, groupId, 0, 10);

        assertEquals(1, result.getTotalElements());
        TestHistoryDisplayResponse response = result.getContent().get(0);
        assertEquals("Test 1", response.getTestName());
        assertEquals(testInstance.getActivationTime(), response.getActivationTime());
        assertEquals(testInstance.getEndTime(), response.getEndTime());
        assertEquals(testInstance.getTimeDuration(), response.getTimeDuration());
        assertEquals(80, response.getScore());
        assertEquals(100, response.getTotalScore());
    }

    @Test
    void shouldReturnEmptyPageWhenNoTestHistoryExistsForUserInGroup() {
        Integer groupId = 1;
        User user = new User();
        user.setId(1);

        UserGroup userGroup = new UserGroup();
        when(userGroupRepository.findByUserAndGroupId(user, groupId)).thenReturn(Optional.of(userGroup));

        Pageable pageable = PageRequest.of(0, 10);
        Page<TestHistory> testHistoryPage = new PageImpl<>(List.of(), pageable, 0);
        when(testHistoryRepository.findByUserAndTestInstanceGroupId(user, groupId, pageable)).thenReturn(testHistoryPage);

        Page<TestHistoryDisplayResponse> result = testHistoryService.getTestHistoryForDisplay(user, groupId, 0, 10);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void shouldHandlePaginationCorrectlyWhenPageSizeIsLarge() {
        Integer groupId = 1;
        User user = new User();
        user.setId(1);

        TestInstance testInstance = new TestInstance();
        TestTemplate testTemplate = new TestTemplate();
        testTemplate.setName("Test 1");
        testTemplate.setTotalScore(100);

        testInstance.setTestTemplate(testTemplate);
        testInstance.setActivationTime(LocalDateTime.now().minusDays(1));
        testInstance.setEndTime(LocalDateTime.now());
        testInstance.setTimeDuration(60);

        TestHistory testHistory = new TestHistory();
        testHistory.setTestInstance(testInstance);
        testHistory.setScore(80);

        UserGroup userGroup = new UserGroup();
        when(userGroupRepository.findByUserAndGroupId(user, groupId)).thenReturn(Optional.of(userGroup));

        Pageable pageable = PageRequest.of(0, 100);
        Page<TestHistory> testHistoryPage = new PageImpl<>(List.of(testHistory), pageable, 1);
        when(testHistoryRepository.findByUserAndTestInstanceGroupId(user, groupId, pageable)).thenReturn(testHistoryPage);

        Page<TestHistoryDisplayResponse> result = testHistoryService.getTestHistoryForDisplay(user, groupId, 0, 100);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void shouldReturnEmptyListWhenNoTestHistoriesExist() {
        Integer testInstanceId = 1;
        Integer groupId = 1;
        User currentUser = new User();
        currentUser.setId(1);

        when(userGroupRepository.existsByGroupIdAndUserIdAndIsOwnerTrue(groupId, currentUser.getId())).thenReturn(true);
        when(testHistoryRepository.findByTestInstanceIdAndTestInstanceGroupId(testInstanceId, groupId)).thenReturn(List.of());

        List<TestResultDto> result = testHistoryService.getTestResults(testInstanceId, groupId, currentUser);

        assertTrue(result.isEmpty());
    }
}
