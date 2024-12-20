package com.learning.english;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learning.english.models.*;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.UserAnnouncementRepository;
import com.learning.english.service.UserAnnouncementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class UserAnnouncementServiceTest {
    @Mock
    private UserAnnouncementRepository userAnnouncementRepository;

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private UserAnnouncementService userAnnouncementService;

    @Test
    void shouldReturnUnseenAnnouncementsCountIfUserBelongsToGroup() {
        User user = new User();
        user.setId(1);
        user.setFirstName("John");
        user.setLastName("Doe");

        Group group = new Group();
        group.setId(1);

        UserGroup userGroup = new UserGroup();
        userGroup.setUser(user);
        group.setUserGroups(List.of(userGroup));

        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(userAnnouncementRepository.countUnseenAnnouncementsByUserAndGroup(user.getId(), group.getId())).thenReturn(5L);

        Long unseenCount = userAnnouncementService.countUnseenAnnouncements(user, 1);

        assertEquals(5L, unseenCount);
    }

    @Test
    void shouldThrowExceptionIfGroupNotFound() {
        User user = new User();
        user.setId(1);
        user.setFirstName("John");
        user.setLastName("Doe");

        when(groupRepository.findById(1)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userAnnouncementService.countUnseenAnnouncements(user, 1);
        });

        assertEquals("Group not found", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionIfUserDoesNotBelongToGroup() {
        User user = new User();
        user.setId(1);
        user.setFirstName("John");
        user.setLastName("Doe");

        Group group = new Group();
        group.setId(1);

        UserGroup otherUserGroup = new UserGroup();
        User otherUser = new User();
        otherUser.setId(2);
        otherUserGroup.setUser(otherUser);
        group.setUserGroups(List.of(otherUserGroup));

        when(groupRepository.findById(1)).thenReturn(Optional.of(group));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userAnnouncementService.countUnseenAnnouncements(user, 1);
        });

        assertEquals("User does not belong to the specified group", exception.getMessage()); // Ensure the exception message is correct
    }
}
