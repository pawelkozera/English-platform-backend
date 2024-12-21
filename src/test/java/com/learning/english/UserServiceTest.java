package com.learning.english;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learning.english.dto.GroupResponse;
import com.learning.english.dto.UserProfileResponse;
import com.learning.english.models.*;
import com.learning.english.repository.UserGroupRepository;
import com.learning.english.repository.UserRepository;
import com.learning.english.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldThrowUsernameNotFoundExceptionWhenUserNotFound() {
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.userDetailsService().loadUserByUsername(email);
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void shouldReturnUserWhenFound() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setFirstName("John");
        user.setLastName("Doe");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetailsService userDetailsService = userService.userDetailsService();
        User foundUser = (User) userDetailsService.loadUserByUsername(email);

        assertEquals(email, foundUser.getEmail());
        assertEquals("John", foundUser.getFirstName());
        assertEquals("Doe", foundUser.getLastName());
    }

    @Test
    void shouldReturnUserProfileResponse() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        UserProfileResponse profile = userService.getUserProfileByEmail(user);

        assertNotNull(profile);
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("John", profile.getFirstName());
        assertEquals("Doe", profile.getLastName());
    }

    @Test
    void shouldReturnUserIfEmailExists() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        User foundUser = userService.findByEmail(email);

        assertNotNull(foundUser);
        assertEquals(email, foundUser.getEmail());
    }

    @Test
    void shouldReturnNullIfEmailNotFound() {
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        User foundUser = userService.findByEmail(email);

        assertNull(foundUser);
    }

    @Test
    void shouldReturnAllUserGroups() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(1);
        group.setGroupName("Test Group");
        group.setGroupCode("TEST123");

        UserGroup userGroup = new UserGroup();
        userGroup.setGroup(group);
        userGroup.setOwner(true);
        userGroup.setUser(user);

        List<UserGroup> userGroups = Collections.singletonList(userGroup);
        when(userGroupRepository.findByUser(user)).thenReturn(userGroups);

        List<GroupResponse> groupResponses = userService.getAllUserGroups(user);

        assertNotNull(groupResponses);
        assertEquals(1, groupResponses.size());
        assertEquals("Test Group", groupResponses.get(0).getGroupName());
        assertEquals("TEST123", groupResponses.get(0).getGroupCode());
        assertTrue(groupResponses.get(0).isOwner());
    }
}

