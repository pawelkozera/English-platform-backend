package com.learning.english.service;

import com.learning.english.dao.GroupCreateRequest;
import com.learning.english.dao.GroupJoinRequest;
import com.learning.english.models.Group;
import com.learning.english.models.User;
import com.learning.english.models.UserGroup;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.UserGroupRepository;
import com.learning.english.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;

    public void createGroup(GroupCreateRequest groupCreateRequest, User user) {
        String groupCode = generateUniqueGroupCode();

        Group group = Group.builder()
                .groupName(groupCreateRequest.getGroupName())
                .password(groupCreateRequest.getPassword())
                .groupCode(groupCode)
                .build();

        groupRepository.save(group);

        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .isOwner(true)
                .build();

        userGroupRepository.save(userGroup);
    }

    private String generateUniqueGroupCode() {
        String groupCode;
        do {
            groupCode = RandomStringUtils.insecure().nextAlphanumeric(4);
        } while (groupRepository.findByGroupCode(groupCode).isPresent());

        return groupCode;
    }

    public void joinGroup(User user, GroupJoinRequest groupRequest) {
        Optional<Group> groupOptional = groupRepository.findByGroupCode(groupRequest.getGroupCode());

        if (groupOptional.isEmpty()) {
            throw new IllegalArgumentException("Group not found");
        }

        Group group = groupOptional.get();

        Optional<UserGroup> userGroupOptional = userGroupRepository.findByUserAndGroup(user, group);
        if (userGroupOptional.isPresent()) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        if (!group.getPassword().equals(groupRequest.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .isOwner(false)
                .build();

        userGroupRepository.save(userGroup);
    }
}
