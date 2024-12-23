package com.learning.english.unit;

import com.learning.english.dto.AnnouncementAddRequest;
import com.learning.english.dto.AnnouncementDisplayResponse;
import com.learning.english.models.*;
import com.learning.english.repository.AnnouncementRepository;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.UserAnnouncementRepository;
import com.learning.english.repository.UserGroupRepository;
import com.learning.english.service.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnnouncementServiceTest {

    private AnnouncementService announcementService;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private UserAnnouncementRepository userAnnouncementRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        announcementService = new AnnouncementService(announcementRepository, groupRepository, userGroupRepository, userAnnouncementRepository);
    }

    @Test
    void testAddAnnouncement_GroupNotFound() {
        AnnouncementAddRequest request = new AnnouncementAddRequest(1, "Title", "Content");
        User user = new User();

        when(groupRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            announcementService.addAnnouncement(request, user);
        });

        verify(groupRepository, times(1)).findById(1);
        verifyNoInteractions(userGroupRepository, announcementRepository);
    }

    @Test
    void testAddAnnouncement_UserNotInGroup() {
        AnnouncementAddRequest request = new AnnouncementAddRequest(1, "Title", "Content");
        User user = new User();
        Group group = new Group();

        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            announcementService.addAnnouncement(request, user);
        });

        verify(groupRepository, times(1)).findById(1);
        verify(userGroupRepository, times(1)).findByUserAndGroup(user, group);
        verifyNoInteractions(announcementRepository);
    }

    @Test
    void testAddAnnouncement_UserNotOwner() {
        AnnouncementAddRequest request = new AnnouncementAddRequest(1, "Title", "Content");
        User user = new User();
        Group group = new Group();
        UserGroup userGroup = mock(UserGroup.class);

        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group)).thenReturn(Optional.of(userGroup));
        when(userGroup.isOwner()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            announcementService.addAnnouncement(request, user);
        });

        verify(groupRepository, times(1)).findById(1);
        verify(userGroupRepository, times(1)).findByUserAndGroup(user, group);
        verifyNoInteractions(announcementRepository);
    }

    @Test
    void testAddAnnouncement_Success() {
        AnnouncementAddRequest request = new AnnouncementAddRequest(1, "Title", "Content");
        User user = new User();
        Group group = new Group();
        UserGroup userGroup = mock(UserGroup.class);

        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group)).thenReturn(Optional.of(userGroup));
        when(userGroup.isOwner()).thenReturn(true);

        announcementService.addAnnouncement(request, user);

        verify(groupRepository, times(1)).findById(1);
        verify(userGroupRepository, times(1)).findByUserAndGroup(user, group);
        verify(announcementRepository, times(1)).save(any(Announcement.class));
    }

    @Test
    void testGetAnnouncementsForDisplay_GroupNotFound() {
        User user = new User();
        when(groupRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            announcementService.getAnnouncementsForDisplay(user, 1, 0, 10);
        });

        verify(groupRepository, times(1)).findById(1);
        verifyNoInteractions(userGroupRepository, announcementRepository, userAnnouncementRepository);
    }

    @Test
    void testGetAnnouncementsForDisplay_UserNotInGroup() {
        User user = new User();
        Group group = new Group();
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            announcementService.getAnnouncementsForDisplay(user, 1, 0, 10);
        });

        verify(groupRepository, times(1)).findById(1);
        verify(userGroupRepository, times(1)).findByUserAndGroup(user, group);
        verifyNoInteractions(announcementRepository, userAnnouncementRepository);
    }

    @Test
    void testGetAnnouncementsForDisplay_Success_NoPreviousUserAnnouncements() {
        User user = new User();
        Group group = new Group();
        Announcement announcement = new Announcement();
        announcement.setId(1L);
        announcement.setTitle("Test Announcement");
        announcement.setContent("This is a test");
        announcement.setCreatedAt(LocalDateTime.now());

        Page<Announcement> announcementsPage = new PageImpl<>(Collections.singletonList(announcement));
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group)).thenReturn(Optional.of(new UserGroup()));
        when(announcementRepository.findByGroupId(1, PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")))))
                .thenReturn(announcementsPage);
        when(userAnnouncementRepository.findByUserAndAnnouncement(user, announcement)).thenReturn(Optional.empty());

        Page<AnnouncementDisplayResponse> result = announcementService.getAnnouncementsForDisplay(user, 1, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("Test Announcement", result.getContent().getFirst().getTitle());
        assertEquals("This is a test", result.getContent().getFirst().getContent());

        verify(userAnnouncementRepository, times(1)).save(any(UserAnnouncement.class));
    }

    @Test
    void testGetAnnouncementsForDisplay_Success_PreviousUserAnnouncement() {
        User user = new User();
        Group group = new Group();
        Announcement announcement = new Announcement();
        announcement.setId(1L);
        announcement.setTitle("Test Announcement");
        announcement.setContent("This is a test");
        announcement.setCreatedAt(LocalDateTime.now());

        UserAnnouncement userAnnouncement = new UserAnnouncement();
        userAnnouncement.setSeen(false);

        Page<Announcement> announcementsPage = new PageImpl<>(Collections.singletonList(announcement));
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group)).thenReturn(Optional.of(new UserGroup()));
        when(announcementRepository.findByGroupId(1, PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")))))
                .thenReturn(announcementsPage);
        when(userAnnouncementRepository.findByUserAndAnnouncement(user, announcement))
                .thenReturn(Optional.of(userAnnouncement));

        Page<AnnouncementDisplayResponse> result = announcementService.getAnnouncementsForDisplay(user, 1, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("Test Announcement", result.getContent().getFirst().getTitle());
        assertEquals("This is a test", result.getContent().getFirst().getContent());
        assertTrue(userAnnouncement.isSeen());

        verify(userAnnouncementRepository, times(1)).save(userAnnouncement);
    }
}