package com.learning.english.service;

import com.learning.english.dao.GroupCreateRequest;
import com.learning.english.models.Group;
import com.learning.english.models.User;
import com.learning.english.models.UserGroup;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.UserGroupRepository;
import com.learning.english.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;

    public void createGroup(GroupCreateRequest groupCreateRequest, User user) {
        Group group = Group.builder()
                .groupName(groupCreateRequest.getGroupName())
                .password(groupCreateRequest.getPassword())
                .build();

        groupRepository.save(group);

        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .isOwner(true)
                .build();

        userGroupRepository.save(userGroup);
    }
}
