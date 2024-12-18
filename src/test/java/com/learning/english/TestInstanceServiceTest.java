package com.learning.english;

import com.learning.english.dto.TestHistoryAddRequest;
import com.learning.english.dto.TestInstanceAddRequest;
import com.learning.english.dto.TestInstanceDisplayResponse;
import com.learning.english.models.*;
import com.learning.english.repository.*;
import com.learning.english.service.TestHistoryService;
import com.learning.english.service.TestInstanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.AssertionErrors.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class TestInstanceServiceTest {
    @Mock
    private TestInstanceRepository testInstanceRepository;

    @Mock
    private TestTemplateRepository testTemplateRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @InjectMocks
    private TestInstanceService testInstanceService;

    private User mockUser;
    private Group group;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setEmail("test@example.com");
        mockUser.setFirstName("John");
        mockUser.setLastName("Doe");

        Group group = new Group();
        group.setId(1);
        group.setGroupName("Test Group");
    }

    @Test
    void shouldThrowExceptionWhenTestTemplateNotFound() {
        TestInstanceAddRequest request = new TestInstanceAddRequest();
        request.setTestTemplateId(1);
        request.setGroupId(1);
        request.setActivationTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setTimeDuration(60);

        when(testTemplateRepository.findById(anyInt())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            testInstanceService.addTestInstance(request, mockUser);
        });
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotOwnerOfTestTemplate() {
        TestTemplate testTemplate = new TestTemplate();
        testTemplate.setOwner(mockUser);
        when(testTemplateRepository.findById(anyInt())).thenReturn(Optional.of(testTemplate));

        TestInstanceAddRequest request = new TestInstanceAddRequest();
        request.setTestTemplateId(1);
        request.setGroupId(1);
        request.setActivationTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setTimeDuration(60);

        assertThrows(RuntimeException.class, () -> {
            testInstanceService.addTestInstance(request, mockUser);
        });
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotOwnerOfGroup() {
        Group group = new Group();
        group.setId(1);

        UserGroup userGroup = new UserGroup();
        userGroup.setOwner(false);

        TestInstanceAddRequest request = new TestInstanceAddRequest();
        request.setTestTemplateId(1);
        request.setGroupId(1);
        request.setActivationTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setTimeDuration(60);

        assertThrows(RuntimeException.class, () -> {
            testInstanceService.addTestInstance(request, mockUser);
        });
    }

    @Test
    void shouldCreateTestInstanceWhenValid() {
        TestTemplate testTemplate = new TestTemplate();
        testTemplate.setOwner(mockUser);
        when(testTemplateRepository.findById(anyInt())).thenReturn(Optional.of(testTemplate));

        Group group = new Group();
        group.setId(1);
        when(groupRepository.findById(anyInt())).thenReturn(Optional.of(group));

        UserGroup userGroup = new UserGroup();
        userGroup.setOwner(true);
        when(userGroupRepository.findByUserAndGroup(any(), any())).thenReturn(Optional.of(userGroup));

        TestInstanceAddRequest request = new TestInstanceAddRequest();
        request.setTestTemplateId(1);
        request.setGroupId(1);
        request.setActivationTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        request.setTimeDuration(60);

        testInstanceService.addTestInstance(request, mockUser);

        verify(testInstanceRepository, times(1)).save(any(TestInstance.class));
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenUserIsNotInGroup() {
        when(userGroupRepository.findByUserAndGroupId(any(), anyInt())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> {
            testInstanceService.getTestInstancesForDisplay(mockUser, 1, 0, 10);
        });
    }

    @Test
    void shouldReturnActiveTestInstancesForUserWithAccess() {
        UserGroup userGroup = new UserGroup();
        userGroup.setOwner(true);

        TestTemplate testTemplate = new TestTemplate();
        testTemplate.setName("Test 1");

        TestInstance testInstance1 = new TestInstance();
        testInstance1.setId(1);
        testInstance1.setUuid(UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"));
        testInstance1.setTestTemplate(testTemplate);
        testInstance1.setActivationTime(LocalDateTime.now().minusMinutes(1));
        testInstance1.setEndTime(LocalDateTime.now().plusMinutes(1));
        testInstance1.setTimeDuration(60);

        TestInstance testInstance2 = new TestInstance();
        testInstance2.setId(2);
        testInstance2.setUuid(UUID.fromString("8dcbf1a4-8cfa-411a-9144-b32c0b4d3177"));
        testInstance2.setTestTemplate(testTemplate);
        testInstance2.setActivationTime(LocalDateTime.now().minusMinutes(5));
        testInstance2.setEndTime(LocalDateTime.now().minusMinutes(1));
        testInstance2.setTimeDuration(60);

        when(userGroupRepository.findByUserAndGroupId(any(), anyInt())).thenReturn(Optional.of(userGroup));
        when(testInstanceRepository.findByGroupId(anyInt(), any())).thenReturn(new PageImpl<>(Arrays.asList(testInstance1, testInstance2)));

        Page<TestInstanceDisplayResponse> result = testInstanceService.getTestInstancesForDisplay(mockUser, 1, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"), result.getContent().get(0).getTestInstanceUUID());
    }
}
