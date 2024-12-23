package com.learning.english.integration;

import com.learning.english.models.*;
import com.learning.english.repository.AnnouncementRepository;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.UserGroupRepository;
import com.learning.english.repository.UserRepository;
import com.learning.english.service.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AnnouncementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    private String jwtToken;

    private int groupId;

    @BeforeEach
    public void setUp() throws Exception {
        User user = User.builder()
                .firstName("test")
                .lastName("test")
                .email("test@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(user);

        jwtToken = jwtService.generateToken(user);

        Group group = Group.builder()
                .groupName("Test Group")
                .password("password123")
                .groupCode("uniqueCode123")
                .build();

        groupRepository.save(group);

        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .isOwner(true)
                .build();

        userGroupRepository.save(userGroup);

        groupId = group.getId();
    }

    @AfterEach
    public void tearDown() {
        announcementRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testAnnouncementSavedInDatabase() throws Exception {
        String announcementJson = "{\"groupId\": " + groupId + ", \"title\": \"Test Announcement\", \"content\": \"This is a test announcement\"}";

        mockMvc.perform(post("/api/v1/announcement/add")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType("application/json")
                        .content(announcementJson))
                .andExpect(status().isOk());

        boolean announcementExists = announcementRepository.existsByTitleAndContent("Test Announcement", "This is a test announcement");
        assertTrue(announcementExists, "Announcement was not saved in the database");
    }

    @Test
    public void testAddAnnouncementByNonMember() throws Exception {
        String announcementJson = "{\"groupId\": " + groupId + ", \"title\": \"Test Announcement\", \"content\": \"This is a test announcement\"}";

        User nonMember = User.builder()
                .firstName("NonMember")
                .lastName("Test")
                .email("nonmember@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(nonMember);

        String nonMemberToken = jwtService.generateToken(nonMember);

        mockMvc.perform(post("/api/v1/announcement/add")
                        .cookie(new Cookie("accessToken", nonMemberToken))
                        .contentType("application/json")
                        .content(announcementJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testErrorMessageForNonOwner() throws Exception {
        String announcementJson = "{\"groupId\": " + groupId + ", \"title\": \"Test Announcement\", \"content\": \"This is a test announcement\"}";

        User member = User.builder()
                .firstName("Member")
                .lastName("Test")
                .email("member@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(member);

        String memberToken = jwtService.generateToken(member);

        UserGroup userGroup = UserGroup.builder()
                .user(member)
                .group(groupRepository.findById(groupId).orElseThrow())
                .isOwner(false)
                .build();
        userGroupRepository.save(userGroup);

        MvcResult result = mockMvc.perform(post("/api/v1/announcement/add")
                        .cookie(new Cookie("accessToken", memberToken))
                        .contentType("application/json")
                        .content(announcementJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("Only the owner can add an announcement"));
    }

    @Test
    public void testGetAnnouncementsForDisplay() throws Exception {
        Announcement announcement = Announcement.builder()
                .group(groupRepository.findById(groupId).orElseThrow())
                .title("Announcement 1")
                .content("This is the first announcement")
                .createdAt(LocalDateTime.now())
                .build();
        announcementRepository.save(announcement);

        MvcResult result = mockMvc.perform(get("/api/v1/announcement/all/for/display/" + groupId)
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("Announcement 1"));
        assertTrue(responseContent.contains("This is the first announcement"));
    }

    @Test
    public void testGetAnnouncementsForDisplayPagination() throws Exception {
        List<Announcement> announcements = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Announcement announcement = Announcement.builder()
                    .group(groupRepository.findById(groupId).orElseThrow())
                    .title("Announcement " + i)
                    .content("Content " + i)
                    .createdAt(LocalDateTime.now())
                    .build();
            announcements.add(announcement);
        }
        announcementRepository.saveAll(announcements);

        MvcResult result = mockMvc.perform(get("/api/v1/announcement/all/for/display/" + groupId)
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("Announcement 3"));
        assertTrue(responseContent.contains("Announcement 2"));
        assertFalse(responseContent.contains("Announcement 1"));
    }

    @Test
    public void testGetAnnouncementsForDisplayAccessDeniedForNonMember() throws Exception {
        User nonMember = User.builder()
                .firstName("NonMember")
                .lastName("Test")
                .email("nonmember@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(nonMember);

        String nonMemberToken = jwtService.generateToken(nonMember);

        mockMvc.perform(get("/api/v1/announcement/all/for/display/" + groupId)
                        .cookie(new Cookie("accessToken", nonMemberToken)))
                .andExpect(status().isBadRequest());
    }
}