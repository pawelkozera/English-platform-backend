package com.learning.english.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.english.dto.RepetitionAddRequest;
import com.learning.english.dto.RepetitionDisplayRequest;
import com.learning.english.dto.RepetitionUpdateRequest;
import com.learning.english.models.*;
import com.learning.english.repository.*;
import com.learning.english.service.JwtService;
import com.learning.english.service.RefreshTokenService;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RepetitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private RepetitionRepository repetitionRepository;

    @Autowired
    private RepetitionWordRepository repetitionWordRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private JwtService jwtService;

    private User user;
    private Group group;
    private Word word;
    private Word word2;
    private RepetitionWord repetitionWord;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("password")
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        jwtToken = jwtService.generateToken(user);

        group = Group.builder()
                .groupName("Test Group")
                .build();
        group = groupRepository.save(group);

        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .isOwner(true)
                .build();
        userGroupRepository.save(userGroup);

        word = Word.builder()
                .word("Test Word")
                .translation("Test Translation")
                .createdBy(user)
                .build();

        word2 = Word.builder()
                .word("Test Word")
                .translation("Test Translation")
                .createdBy(user)
                .build();

        List<Word> words = List.of(word, word2);
        wordRepository.saveAll(words);

        Repetition repetition = repetitionRepository.save(Repetition.builder().student(user).group(group).build());
        repetitionWord = RepetitionWord.builder()
                .repetition(repetition)
                .word(word2)
                .eFactor(2.5)
                .interval(1)
                .nextReviewDate(LocalDate.now().plusDays(0))
                .build();
        repetitionWordRepository.save(repetitionWord);
    }

    @AfterEach
    void tearDown() {
        repetitionWordRepository.deleteAll();
        repetitionRepository.deleteAll();
        wordRepository.deleteAll();
        userGroupRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldAddRepetitionSuccessfully() throws Exception {
        RepetitionAddRequest request = new RepetitionAddRequest();
        request.setWordId(word.getId());
        request.setGroupId(group.getId());

        mockMvc.perform(post("/api/v1/repetition/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Repetition added successfully"));

        assertTrue(repetitionRepository.findByStudent(user).isPresent());
        assertTrue(repetitionWordRepository.existsByRepetitionStudentAndWord(user, word));
    }

    @Test
    void shouldFailWhenWordNotFound() throws Exception {
        RepetitionAddRequest request = new RepetitionAddRequest();
        request.setWordId(999L);
        request.setGroupId(group.getId());

        mockMvc.perform(post("/api/v1/repetition/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Word not found"));
    }

    @Test
    void shouldFailWhenGroupNotFound() throws Exception {
        RepetitionAddRequest request = new RepetitionAddRequest();
        request.setWordId(word.getId());
        request.setGroupId(999);

        mockMvc.perform(post("/api/v1/repetition/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Group not found"));
    }

    @Test
    void shouldFailWhenUserNotInGroup() throws Exception {
        Group anotherGroup = groupRepository.save(Group.builder().groupName("Another Group").build());

        RepetitionAddRequest request = new RepetitionAddRequest();
        request.setWordId(word.getId());
        request.setGroupId(anotherGroup.getId());

        mockMvc.perform(post("/api/v1/repetition/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User does not belong to the specified group"));
    }

    @Test
    void shouldFailWhenRepetitionAlreadyExists() throws Exception {
        Repetition repetition = repetitionRepository.save(Repetition.builder().student(user).group(group).build());
        RepetitionWord repetitionWord = RepetitionWord.builder()
                .repetition(repetition)
                .word(word)
                .eFactor(2.5)
                .interval(1)
                .nextReviewDate(LocalDate.now().plusDays(1))
                .build();
        repetitionWordRepository.save(repetitionWord);

        RepetitionAddRequest request = new RepetitionAddRequest();
        request.setWordId(word.getId());
        request.setGroupId(group.getId());

        mockMvc.perform(post("/api/v1/repetition/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request))
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Repetition for this word already exists"));
    }

    @Test
    void shouldRemoveRepetitionSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/repetition/remove/{wordId}", word2.getId())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Repetition removed successfully"));

        Optional<RepetitionWord> repetitionWordCheck = repetitionWordRepository.findByRepetition_StudentAndWord_Id(user, Math.toIntExact(word.getId()));
        assertThat(repetitionWordCheck).isEmpty();

        Optional<Repetition> repetitionCheck = repetitionRepository.findByStudent(user);
        assertThat(repetitionCheck).isEmpty();
    }

    @Test
    void shouldReturnErrorWhenRepetitionDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/v1/repetition/remove/{wordId}", 999)
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Repetition word not found"));
    }

    @Test
    void shouldReturnErrorWhenUserNotInGroup() throws Exception {
        User anotherUser = userRepository.save(User.builder()
                .email("otheruser@example.com")
                .password("password")
                .role(Role.USER)
                .build());

        String anotherJwt = jwtService.generateToken(anotherUser);

        mockMvc.perform(delete("/api/v1/repetition/remove/{wordId}", word.getId())
                        .cookie(new Cookie("accessToken", anotherJwt)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Repetition word not found"));
    }

    @Test
    void shouldReturnTrueIfWordExistsInRepetitions() throws Exception {
        Repetition repetition = repetitionRepository.save(Repetition.builder().student(user).group(group).build());
        RepetitionWord repetitionWord = RepetitionWord.builder()
                .repetition(repetition)
                .word(word)
                .eFactor(2.5)
                .interval(1)
                .nextReviewDate(LocalDate.now().plusDays(1))
                .build();
        repetitionWordRepository.save(repetitionWord);

        mockMvc.perform(get("/api/v1/repetition/exists/{wordId}", word.getId())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void shouldReturnFalseIfWordDoesNotExistInRepetitions() throws Exception {
        Word anotherWord = wordRepository.save(Word.builder()
                .word("another")
                .translation("another translation")
                .createdBy(user)
                .build());

        mockMvc.perform(get("/api/v1/repetition/exists/{wordId}", anotherWord.getId())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    void shouldReturnErrorIfWordDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/repetition/exists/{wordId}", 999L)
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Word not found"));
    }

    @Test
    void shouldReturnRepetitionsCountForToday() throws Exception {
        repetitionWordRepository.deleteAll();

        Repetition repetition = repetitionRepository.save(Repetition.builder().student(user).group(group).build());
        RepetitionWord repetitionWord = RepetitionWord.builder()
                .repetition(repetition)
                .word(word)
                .eFactor(2.5)
                .interval(1)
                .nextReviewDate(LocalDate.now().plusDays(0))
                .build();
        repetitionWordRepository.save(repetitionWord);

        mockMvc.perform(get("/api/v1/repetition/count/today/{groupId}", group.getId())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1L));
    }

    @Test
    void shouldReturnZeroWhenNoRepetitionsForToday() throws Exception {
        repetitionWordRepository.deleteAll();

        mockMvc.perform(get("/api/v1/repetition/count/today/{groupId}", group.getId())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0L));
    }

    @Test
    void shouldReturnRepetitionWordsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/repetition/words")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .param("groupId", String.valueOf(group.getId()))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].word").value("Test Word"))
                .andExpect(jsonPath("$[0].translation").value("Test Translation"));
    }

    @Test
    void shouldUpdateRepetitionSuccessfully() throws Exception {
        List<RepetitionUpdateRequest> updateRequests = new ArrayList<>();
        updateRequests.add(new RepetitionUpdateRequest(repetitionWord.getId(), 2));

        mockMvc.perform(post("/api/v1/repetition/update")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateRequests)))
                .andExpect(status().isOk())
                .andExpect(content().string("Repetitions updated successfully"));
    }

    @Test
    void shouldReturnForbiddenWhenUserDoesNotHavePermission() throws Exception {
        List<RepetitionUpdateRequest> updateRequests = new ArrayList<>();
        updateRequests.add(new RepetitionUpdateRequest(repetitionWord.getId(), 2));

        User anotherUser = userRepository.save(User.builder()
                .email("otheruser@example.com")
                .password("password")
                .role(Role.USER)
                .build());

        String anotherJwt = jwtService.generateToken(anotherUser);

        mockMvc.perform(post("/api/v1/repetition/update")
                        .cookie(new Cookie("accessToken", anotherJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateRequests)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User does not have permission to update this repetition word"));
    }

    @Test
    void shouldReturnBadRequestWhenGradeIsLessThanOne() throws Exception {
        List<RepetitionUpdateRequest> updateRequests = new ArrayList<>();
        updateRequests.add(new RepetitionUpdateRequest(repetitionWord.getId(), 0));

        mockMvc.perform(post("/api/v1/repetition/update")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateRequests)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid grade"));
    }

    @Test
    void shouldReturnBadRequestWhenGradeIsGreaterThanThree() throws Exception {
        List<RepetitionUpdateRequest> updateRequests = new ArrayList<>();
        updateRequests.add(new RepetitionUpdateRequest(repetitionWord.getId(), 4));

        mockMvc.perform(post("/api/v1/repetition/update")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateRequests)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid grade"));
    }

    @Test
    void shouldUpdateEFactorIntervalAndNextReviewDate() throws Exception {
        List<RepetitionUpdateRequest> updateRequests = new ArrayList<>();
        updateRequests.add(new RepetitionUpdateRequest(repetitionWord.getId(), 2));

        mockMvc.perform(post("/api/v1/repetition/update")
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateRequests)))
                .andExpect(status().isOk());

        RepetitionWord updatedRepetitionWord = repetitionWordRepository.findById(repetitionWord.getId()).orElseThrow();
        assertThat(updatedRepetitionWord.getEFactor()).isGreaterThan(1.3);
        assertThat(updatedRepetitionWord.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(updatedRepetitionWord.getInterval()));
    }
}