package com.learning.english.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.english.dto.TemplateAddRequest;
import com.learning.english.models.*;
import com.learning.english.repository.*;
import com.learning.english.service.JwtService;
import com.learning.english.service.TestTemplateService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserAnnouncementControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserAnnouncementRepository userAnnouncementRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private JwtService jwtService;

    private String jwtToken;
    private String nonGroupMemberJwtToken;
    private int groupId;
    private int userId;

    @BeforeEach
    public void setUp() throws Exception {
        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("testuser@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        jwtToken = jwtService.generateToken(user);

        User nonGroupMember = User.builder()
                .firstName("Non")
                .lastName("Member")
                .email("nongroupmember@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        nonGroupMemberJwtToken = jwtService.generateToken(nonGroupMember);

        List<User> users = List.of(user, nonGroupMember);
        List<User> savedUsers = userRepository.saveAll(users);
        userId = savedUsers.getFirst().getId();

        Group group = Group.builder()
                .groupName("Test Group")
                .build();
        groupRepository.save(group);
        groupId = group.getId();

        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .isOwner(true)
                .build();
        userGroupRepository.save(userGroup);

        Announcement announcement = Announcement.builder()
                .title("Test Announcement")
                .group(group)
                .createdAt(LocalDateTime.now())
                .content("Test content")
                .build();

        Announcement announcement2 = Announcement.builder()
                .title("Test Announcement 2")
                .group(group)
                .createdAt(LocalDateTime.now())
                .content("Test content 2")
                .build();

        List<Announcement> announcements = List.of(announcement, announcement2);
        announcementRepository.saveAll(announcements);

        UserAnnouncement userAnnouncement = UserAnnouncement.builder()
                .announcement(announcement)
                .user(user)
                .seen(false)
                .seenAt(LocalDateTime.now())
                .build();

        UserAnnouncement userAnnouncement1 = UserAnnouncement.builder()
                .announcement(announcement)
                .user(user)
                .seen(false)
                .seenAt(LocalDateTime.now())
                .build();

        userAnnouncementRepository.saveAll(List.of(userAnnouncement, userAnnouncement1));
    }

    @AfterEach
    public void tearDown() {
        userAnnouncementRepository.deleteAll();
        announcementRepository.deleteAll();
        userGroupRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnCountOfUnseenAnnouncements() throws Exception {
        mockMvc.perform(get("/api/v1/userAnnouncement/count/unseen/{groupId}", groupId)
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));
    }

    @Test
    void shouldReturnBadRequestIfGroupDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/userAnnouncement/count/unseen/{groupId}", 9999)
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Group not found"));
    }

    @Test
    void shouldReturnBadRequestIfUserIsNotInGroup() throws Exception {
        mockMvc.perform(get("/api/v1/userAnnouncement/count/unseen/{groupId}", groupId)
                        .cookie(new Cookie("accessToken", nonGroupMemberJwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User does not belong to the specified group"));
    }

    @Test
    void shouldReturnUnauthorizedForMissingAuthToken() throws Exception {
        mockMvc.perform(get("/api/v1/userAnnouncement/count/unseen/{groupId}", groupId))
                .andExpect(status().isUnauthorized());
    }
}
