package com.learning.english.service;

import com.learning.english.dto.TaskDisplayResponse;
import com.learning.english.dto.TestInstanceAddRequest;
import com.learning.english.dto.TestInstanceDisplayResponse;
import com.learning.english.models.*;
import com.learning.english.repository.TestInstanceRepository;
import com.learning.english.repository.TestTemplateRepository;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestInstanceService {
    private final TestInstanceRepository testInstanceRepository;
    private final TestTemplateRepository testTemplateRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;

    public void addTestInstance(TestInstanceAddRequest testInstanceAddRequest, User user) {
        TestTemplate testTemplate = testTemplateRepository.findById(testInstanceAddRequest.getTestTemplateId())
                .orElseThrow(() -> new RuntimeException("TestTemplate not found"));

        if (!testTemplate.getOwner().equals(user)) {
            throw new RuntimeException("You are not the owner of this TestTemplate");
        }

        Group group = groupRepository.findById(testInstanceAddRequest.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Optional<UserGroup> userGroupOptional = userGroupRepository.findByUserAndGroup(user, group);
        if (userGroupOptional.isEmpty() || !userGroupOptional.get().isOwner()) {
            throw new RuntimeException("You must be the owner of the group to create a test instance.");
        }

        TestInstance testInstance = TestInstance.builder()
                .testTemplate(testTemplate)
                .group(group)
                .activationTime(testInstanceAddRequest.getActivationTime())
                .endTime(testInstanceAddRequest.getEndTime())
                .uuid(UUID.randomUUID())
                .build();

        testInstanceRepository.save(testInstance);
    }


    public Page<TestInstanceDisplayResponse> getTestInstancesForDisplay(User user, Integer groupId, int page, int size) {
        Optional<UserGroup> userGroupOptional = userGroupRepository.findByUserAndGroupId(user, groupId);
        if (userGroupOptional.isEmpty()) {
            throw new AccessDeniedException("You do not have access to this group.");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<TestInstance> testInstances = testInstanceRepository.findByGroupId(groupId, pageable);

        List<TestInstanceDisplayResponse> displayResponses = testInstances.getContent().stream()
                .map(testInstance -> TestInstanceDisplayResponse.builder()
                        .testInstanceId(testInstance.getId())
                        .testInstanceUUID(testInstance.getUuid())
                        .testName(testInstance.getTestTemplate().getName())
                        .activationTime(testInstance.getActivationTime())
                        .endTime(testInstance.getEndTime())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(displayResponses, pageable, testInstances.getTotalElements());
    }

    public List<Integer> getTasksForTestInstance(User user, Integer testInstanceId) {
        TestInstance testInstance = testInstanceRepository.findById(testInstanceId)
                .orElseThrow(() -> new RuntimeException("TestInstance not found"));

        Optional<UserGroup> userGroupOptional = userGroupRepository.findByUserAndGroupId(user, testInstance.getGroup().getId());
        if (userGroupOptional.isEmpty()) {
            throw new AccessDeniedException("You do not have access to this test instance.");
        }

        return testInstance.getTestTemplate().getTasks().stream()
                .map(Task::getId)
                .collect(Collectors.toList());
    }
}
