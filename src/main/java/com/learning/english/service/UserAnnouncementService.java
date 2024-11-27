package com.learning.english.service;

import com.learning.english.models.Group;
import com.learning.english.models.User;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.UserAnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAnnouncementService {
    private final UserAnnouncementRepository userAnnouncementRepository;
    private final GroupRepository groupRepository;

    public Long countUnseenAnnouncements(User user, Integer groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        boolean userInGroup = group.getUserGroups().stream()
                .anyMatch(userGroup -> userGroup.getUser().equals(user));

        if (!userInGroup) {
            throw new IllegalArgumentException("User does not belong to the specified group");
        }

        return userAnnouncementRepository.countUnseenAnnouncementsByUserAndGroup(user.getId(), groupId);
    }
}
