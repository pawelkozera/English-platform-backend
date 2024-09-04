package com.learning.english.service;

import com.learning.english.dao.GroupCreateRequest;
import com.learning.english.models.Group;
import com.learning.english.models.User;
import com.learning.english.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;

    public void createGroup(GroupCreateRequest groupCreateRequest, User user) {
        Group group = Group.builder()
                .groupName(groupCreateRequest.getGroupName())
                .password(groupCreateRequest.getPassword())
                .build();

        group.getStudents().add(user);

        groupRepository.save(group);
    }
}
