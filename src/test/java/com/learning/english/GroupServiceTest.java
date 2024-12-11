package com.learning.english;

import com.learning.english.dto.GroupCreateRequest;
import com.learning.english.dto.GroupJoinRequest;
import com.learning.english.models.Group;
import com.learning.english.models.Lesson;
import com.learning.english.models.User;
import com.learning.english.models.UserGroup;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.LessonRepository;
import com.learning.english.repository.UserGroupRepository;
import com.learning.english.service.GroupService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupServiceTest {

    @InjectMocks
    private GroupService groupService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private LessonRepository lessonRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createGroup_ShouldSaveGroupAndUserGroup() {
        // Arrange
        GroupCreateRequest groupCreateRequest = new GroupCreateRequest();
        groupCreateRequest.setGroupName("Test Group");
        groupCreateRequest.setPassword("securePassword");

        User user = User.builder()
                .id(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        String initialGroupCode = "XYZ123";
        String uniqueGroupCode = "ABC123";

        when(groupRepository.findByGroupCode(initialGroupCode)).thenReturn(Optional.of(new Group()));
        when(groupRepository.findByGroupCode(uniqueGroupCode)).thenReturn(Optional.empty());

        when(groupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<RandomStringUtils> mockedRandomStringUtils = mockStatic(RandomStringUtils.class)) {
            RandomStringUtils mockedInstance = mock(RandomStringUtils.class);
            mockedRandomStringUtils.when(RandomStringUtils::insecure).thenReturn(mockedInstance);
            when(mockedInstance.nextAlphanumeric(4))
                    .thenReturn(initialGroupCode)
                    .thenReturn(uniqueGroupCode);

            groupService.createGroup(groupCreateRequest, user);

            ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
            verify(groupRepository, times(1)).save(groupCaptor.capture());
            Group savedGroup = groupCaptor.getValue();
            assertNotNull(savedGroup);
            assertEquals("Test Group", savedGroup.getGroupName());
            assertEquals("securePassword", savedGroup.getPassword());
            assertEquals(uniqueGroupCode, savedGroup.getGroupCode());

            ArgumentCaptor<UserGroup> userGroupCaptor = ArgumentCaptor.forClass(UserGroup.class);
            verify(userGroupRepository, times(1)).save(userGroupCaptor.capture());
            UserGroup savedUserGroup = userGroupCaptor.getValue();
            assertNotNull(savedUserGroup);
            assertEquals(user, savedUserGroup.getUser());
            assertEquals(savedGroup, savedUserGroup.getGroup());
            assertTrue(savedUserGroup.isOwner());
        }
    }

    @Test
    void createGroup_ShouldGenerateUniqueGroupCode() {
        GroupCreateRequest groupCreateRequest = new GroupCreateRequest();
        groupCreateRequest.setGroupName("Unique Code Group");
        groupCreateRequest.setPassword("anotherPassword");

        User user = User.builder()
                .id(2)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice.smith@example.com")
                .build();

        when(groupRepository.findByGroupCode(anyString())).thenReturn(Optional.empty());
        when(groupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        groupService.createGroup(groupCreateRequest, user);

        verify(groupRepository, atLeastOnce()).findByGroupCode(anyString());
    }

    @Test
    void createGroup_ShouldAssignUserAsOwner() {
        GroupCreateRequest groupCreateRequest = new GroupCreateRequest();
        groupCreateRequest.setGroupName("Owner Test Group");
        groupCreateRequest.setPassword("ownerPassword");

        User user = User.builder()
                .id(3)
                .firstName("Bob")
                .lastName("Brown")
                .email("bob.brown@example.com")
                .build();

        when(groupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        groupService.createGroup(groupCreateRequest, user);

        ArgumentCaptor<UserGroup> userGroupCaptor = ArgumentCaptor.forClass(UserGroup.class);
        verify(userGroupRepository, times(1)).save(userGroupCaptor.capture());
        UserGroup savedUserGroup = userGroupCaptor.getValue();
        assertEquals(user, savedUserGroup.getUser());
        assertTrue(savedUserGroup.isOwner());
    }

    @Test
    void joinGroup_GroupNotFound_ShouldThrowException() {
        User user = new User();
        GroupJoinRequest groupRequest = new GroupJoinRequest();
        groupRequest.setGroupCode("TEST123");
        groupRequest.setPassword("password");

        when(groupRepository.findByGroupCode(groupRequest.getGroupCode()))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> groupService.joinGroup(user, groupRequest)
        );

        assertEquals("Group not found", exception.getMessage());
    }

    @Test
    void joinGroup_UserAlreadyMember_ShouldThrowException() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(1);

        GroupJoinRequest groupRequest = new GroupJoinRequest();
        groupRequest.setGroupCode("TEST123");
        groupRequest.setPassword("password");

        when(groupRepository.findByGroupCode(groupRequest.getGroupCode()))
                .thenReturn(Optional.of(group));

        when(userGroupRepository.findByUserAndGroup(user, group))
                .thenReturn(Optional.of(new UserGroup()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> groupService.joinGroup(user, groupRequest)
        );

        assertEquals("User is already a member of this group", exception.getMessage());
    }

    @Test
    void joinGroup_InvalidPassword_ShouldThrowException() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(1);
        group.setPassword("correctPassword");

        GroupJoinRequest groupRequest = new GroupJoinRequest();
        groupRequest.setGroupCode("TEST123");
        groupRequest.setPassword("wrongPassword");

        when(groupRepository.findByGroupCode(groupRequest.getGroupCode()))
                .thenReturn(Optional.of(group));

        when(userGroupRepository.findByUserAndGroup(user, group))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> groupService.joinGroup(user, groupRequest)
        );

        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    void joinGroup_ValidRequest_ShouldSaveUserGroup() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(1);
        group.setPassword("correctPassword");

        GroupJoinRequest groupRequest = new GroupJoinRequest();
        groupRequest.setGroupCode("TEST123");
        groupRequest.setPassword("correctPassword");

        when(groupRepository.findByGroupCode(groupRequest.getGroupCode()))
                .thenReturn(Optional.of(group));

        when(userGroupRepository.findByUserAndGroup(user, group))
                .thenReturn(Optional.empty());

        groupService.joinGroup(user, groupRequest);

        ArgumentCaptor<UserGroup> userGroupCaptor = ArgumentCaptor.forClass(UserGroup.class);
        verify(userGroupRepository, times(1)).save(userGroupCaptor.capture());

        UserGroup savedUserGroup = userGroupCaptor.getValue();
        assertNotNull(savedUserGroup);
        assertEquals(user, savedUserGroup.getUser());
        assertEquals(group, savedUserGroup.getGroup());
        assertFalse(savedUserGroup.isOwner());
    }

    @Test
    void addLessonsToGroup_GroupNotFound_ShouldThrowException() {
        User user = new User();
        Integer groupId = 1;
        List<Integer> lessonIds = List.of(1, 2, 3);

        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> groupService.addLessonsToGroup(user, groupId, lessonIds)
        );

        assertEquals("Group not found", exception.getMessage());
    }

    @Test
    void addLessonsToGroup_UserNotOwner_ShouldThrowException() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(1);

        Integer groupId = 1;
        List<Integer> lessonIds = List.of(1, 2, 3);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group))
                .thenReturn(Optional.of(UserGroup.builder()
                        .user(user)
                        .group(group)
                        .isOwner(false)
                        .build()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> groupService.addLessonsToGroup(user, groupId, lessonIds)
        );

        assertEquals("Only the group owner can add lessons", exception.getMessage());
    }

    @Test
    void addLessonsToGroup_LessonsNotFound_ShouldThrowException() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(1);

        Integer groupId = 1;
        List<Integer> lessonIds = List.of(1, 2, 3);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group))
                .thenReturn(Optional.of(UserGroup.builder()
                        .user(user)
                        .group(group)
                        .isOwner(true)
                        .build()));
        when(lessonRepository.findAllById(lessonIds)).thenReturn(List.of(new Lesson()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> groupService.addLessonsToGroup(user, groupId, lessonIds)
        );

        assertEquals("Some lessons do not exist", exception.getMessage());
    }

    @Test
    void addLessonsToGroup_ValidRequest_ShouldAddLessonsToGroup() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(1);
        group.setLessons(new ArrayList<>());

        Lesson lesson1 = new Lesson();
        lesson1.setId(1);
        lesson1.setGroups(new ArrayList<>());

        Lesson lesson2 = new Lesson();
        lesson2.setId(2);
        lesson2.setGroups(new ArrayList<>());

        Integer groupId = 1;
        List<Integer> lessonIds = List.of(1, 2);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group))
                .thenReturn(Optional.of(UserGroup.builder()
                        .user(user)
                        .group(group)
                        .isOwner(true)
                        .build()));
        when(lessonRepository.findAllById(lessonIds)).thenReturn(List.of(lesson1, lesson2));

        boolean result = groupService.addLessonsToGroup(user, groupId, lessonIds);

        assertTrue(result);
        assertEquals(2, group.getLessons().size());
        assertTrue(group.getLessons().contains(lesson1));
        assertTrue(group.getLessons().contains(lesson2));
        assertTrue(lesson1.getGroups().contains(group));
        assertTrue(lesson2.getGroups().contains(group));

        verify(groupRepository, times(1)).save(group);
    }

    @Test
    void addLessonsToGroup_LessonsAlreadyInGroup_ShouldNotDuplicateLessons() {
        User user = new User();
        user.setId(1);

        Group group = new Group();
        group.setId(1);

        Lesson lesson1 = new Lesson();
        lesson1.setId(1);
        lesson1.setGroups(new ArrayList<>());
        lesson1.getGroups().add(group);

        Lesson lesson2 = new Lesson();
        lesson2.setId(2);
        lesson2.setGroups(new ArrayList<>());

        group.setLessons(new ArrayList<>(List.of(lesson1)));

        Integer groupId = 1;
        List<Integer> lessonIds = List.of(1, 2);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group))
                .thenReturn(Optional.of(UserGroup.builder()
                        .user(user)
                        .group(group)
                        .isOwner(true)
                        .build()));
        when(lessonRepository.findAllById(lessonIds)).thenReturn(List.of(lesson1, lesson2));

        boolean result = groupService.addLessonsToGroup(user, groupId, lessonIds);

        assertTrue(result);
        assertEquals(2, group.getLessons().size());
        assertTrue(group.getLessons().contains(lesson1));
        assertTrue(group.getLessons().contains(lesson2));
        assertTrue(lesson1.getGroups().contains(group));
        assertTrue(lesson2.getGroups().contains(group));

        verify(groupRepository, times(1)).save(group);
    }
}

