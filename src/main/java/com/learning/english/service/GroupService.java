package com.learning.english.service;

import com.learning.english.dto.GroupCreateRequest;
import com.learning.english.dto.GroupJoinRequest;
import com.learning.english.models.Group;
import com.learning.english.models.Lesson;
import com.learning.english.models.User;
import com.learning.english.models.UserGroup;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.LessonRepository;
import com.learning.english.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final LessonRepository lessonRepository;

    public void createGroup(GroupCreateRequest groupCreateRequest, User user) {
        if (groupCreateRequest.getGroupName() == null || groupCreateRequest.getGroupName().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be empty");
        }
        if (groupCreateRequest.getPassword() == null || groupCreateRequest.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

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

    public boolean addLessonsToGroup(User user, Integer groupId, List<Integer> lessonIds) {
        Optional<Group> groupOptional = groupRepository.findById(groupId);
        if (groupOptional.isEmpty()) {
            throw new IllegalArgumentException("Group not found");
        }
        Group group = groupOptional.get();

        Optional<UserGroup> userGroupOptional = userGroupRepository.findByUserAndGroup(user, group);
        if (userGroupOptional.isEmpty() || !userGroupOptional.get().isOwner()) {
            throw new IllegalArgumentException("Only the group owner can add lessons");
        }

        List<Lesson> lessons = lessonRepository.findAllById(lessonIds);
        if (lessons.size() != lessonIds.size()) {
            throw new IllegalArgumentException("Some lessons do not exist");
        }

        for (Lesson lesson : lessons) {
            if (!group.getLessons().contains(lesson)) {
                group.getLessons().add(lesson);
            }
            if (!lesson.getGroups().contains(group)) {
                lesson.getGroups().add(group);
            }
        }

        groupRepository.save(group);
        return true;
    }
}
