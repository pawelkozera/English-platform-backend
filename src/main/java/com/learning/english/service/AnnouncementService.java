package com.learning.english.service;

import com.learning.english.dto.AnnouncementAddRequest;
import com.learning.english.dto.AnnouncementDisplayResponse;
import com.learning.english.dto.LessonsDisplayResponse;
import com.learning.english.models.*;
import com.learning.english.repository.AnnouncementRepository;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.UserAnnouncementRepository;
import com.learning.english.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserAnnouncementRepository userAnnouncementRepository;

    public void addAnnouncement(AnnouncementAddRequest announcementAddRequest, User user) {
        Group group = groupRepository.findById(announcementAddRequest.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        UserGroup userGroup = userGroupRepository.findByUserAndGroup(user, group)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of the group"));

        if (!userGroup.isOwner()) {
            throw new IllegalArgumentException("Only the owner can add an announcement");
        }

        Announcement announcement = Announcement.builder()
                .group(group)
                .title(announcementAddRequest.getTitle())
                .content(announcementAddRequest.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        announcementRepository.save(announcement);
    }

    public Page<AnnouncementDisplayResponse> getAnnouncementsForDisplay(User user, Integer groupId, int page, int size) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        userGroupRepository.findByUserAndGroup(user, group)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of the group"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));

        Page<Announcement> announcements = announcementRepository.findByGroupId(groupId, pageable);

        announcements.forEach(announcement -> {
            userAnnouncementRepository.findByUserAndAnnouncement(user, announcement)
                    .ifPresentOrElse(
                            userAnnouncement -> {
                                if (!userAnnouncement.isSeen()) {
                                    userAnnouncement.setSeen(true);
                                    userAnnouncement.setSeenAt(LocalDateTime.now());
                                    userAnnouncementRepository.save(userAnnouncement);
                                }
                            },
                            () -> {
                                UserAnnouncement newEntry = UserAnnouncement.builder()
                                        .user(user)
                                        .announcement(announcement)
                                        .seen(true)
                                        .seenAt(LocalDateTime.now())
                                        .build();
                                userAnnouncementRepository.save(newEntry);
                            }
                    );
        });

        return announcements.map(announcement -> AnnouncementDisplayResponse.builder()
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .createdAt(announcement.getCreatedAt())
                .build());
    }
}
