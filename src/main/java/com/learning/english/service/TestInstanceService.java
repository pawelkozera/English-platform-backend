package com.learning.english.service;

import com.learning.english.dto.TestInstanceAddRequest;
import com.learning.english.models.*;
import com.learning.english.repository.TestInstanceRepository;
import com.learning.english.repository.TestTemplateRepository;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
                .build();

        testInstanceRepository.save(testInstance);
    }
}
