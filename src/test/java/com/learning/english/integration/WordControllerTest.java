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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthUtil authUtil;

    private String jwtToken;
    private int userId;
    private Word word;

    @BeforeEach
    public void setUp() {
        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("testuser@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        User savedUser = userRepository.save(user);
        userId = savedUser.getId();
        jwtToken = jwtService.generateToken(savedUser);

        Word word1 = Word.builder()
                .word("apple")
                .translation("jabłko")
                .audioFilePath("/audio/apple.mp3")
                .imageFilePath("/images/apple.jpg")
                .createdBy(savedUser)
                .build();

        Word word2 = Word.builder()
                .word("banana")
                .translation("banan")
                .audioFilePath("/audio/banana.mp3")
                .imageFilePath("/images/banana.jpg")
                .createdBy(savedUser)
                .build();

        List<Word> words = (List<Word>) wordRepository.saveAll(List.of(word1, word2));
        word = words.getFirst();
    }

    @AfterEach
    public void tearDown() {
        wordRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnWordsOwnedByUser() throws Exception {
        mockMvc.perform(get("/api/v1/word/all/owned/by/user")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$._embedded.wordResponseList[0].word").value("apple"))
                .andExpect(jsonPath("$._embedded.wordResponseList[0].translation").value("jabłko"))
                .andExpect(jsonPath("$._embedded.wordResponseList[1].word").value("banana"))
                .andExpect(jsonPath("$._embedded.wordResponseList[1].translation").value("banan"));
    }

    @Test
    void shouldReturnEmptyPageIfNoWords() throws Exception {
        wordRepository.deleteAll();

        mockMvc.perform(get("/api/v1/word/all/owned/by/user")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void shouldReturnUnauthorizedIfNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/word/all/owned/by/user")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnPaginatedResults() throws Exception {
        List<Word> words = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Word word = Word.builder()
                    .word("word" + i)
                    .translation("translation" + i)
                    .createdBy(userRepository.findById(userId).orElseThrow())
                    .build();
            words.add(word);
        }
        wordRepository.saveAll(words);

        mockMvc.perform(get("/api/v1/word/all/owned/by/user")
                        .param("page", "0")
                        .param("size", "5")
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(5));
    }

    @Test
    void shouldAddWordWithoutFiles() throws Exception {
        wordRepository.deleteAll();

        MockMultipartHttpServletRequestBuilder requestBuilder = (MockMultipartHttpServletRequestBuilder) multipart("/api/v1/word/add")
                .param("word", "apple")
                .param("translation", "jabłko");

        mockMvc.perform(requestBuilder
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Word created successfully"));

        List<Word> words = (List<Word>) wordRepository.findAll();
        assertEquals(1, words.size());
        assertEquals("apple", words.getFirst().getWord());
        assertEquals("jabłko", words.getFirst().getTranslation());
        assertNull(words.getFirst().getAudioFilePath());
        assertNull(words.getFirst().getImageFilePath());
    }

    @Test
    void shouldAddWordWithFiles() throws Exception {
        wordRepository.deleteAll();

        MockMultipartFile audioFile = new MockMultipartFile("audioFile", "audio.mp3", "audio/mpeg", "audio content".getBytes());
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "image.jpg", "image/jpeg", "image content".getBytes());

        MockMultipartHttpServletRequestBuilder requestBuilder = (MockMultipartHttpServletRequestBuilder) multipart("/api/v1/word/add")
                .file(new MockMultipartFile("wordAddRequest.word", "", "text/plain", "banana".getBytes()))
                .file(new MockMultipartFile("wordAddRequest.translation", "", "text/plain", "banan".getBytes()))
                .file(audioFile)
                .file(imageFile)
                .param("word", "banana")
                .param("translation", "banan");

        mockMvc.perform(requestBuilder
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().string("Word created successfully"));

        List<Word> words = (List<Word>) wordRepository.findAll();
        assertEquals(1, words.size());
        assertEquals("banana", words.getFirst().getWord());
        assertEquals("banan", words.getFirst().getTranslation());
        assertNotNull(words.getFirst().getAudioFilePath());
        assertNotNull(words.getFirst().getImageFilePath());
    }

    @Test
    void shouldFailWithoutAuthentication() throws Exception {
        MockMultipartHttpServletRequestBuilder requestBuilder = multipart("/api/v1/word/add")
                .file(new MockMultipartFile("wordAddRequest.word", "", "text/plain", "orange".getBytes()))
                .file(new MockMultipartFile("wordAddRequest.translation", "", "text/plain", "pomarańcza".getBytes()));

        mockMvc.perform(requestBuilder
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldUpdateWordWithoutFiles() throws Exception {
        MockMultipartHttpServletRequestBuilder requestBuilder = (MockMultipartHttpServletRequestBuilder) multipart("/api/v1/word/update/" + word.getId())
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .param("word", "bananaUpdate")
                .param("translation", "bananUpdate");

        mockMvc.perform(requestBuilder
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().string("Word updated successfully"));

        Word updatedWord = wordRepository.findById(word.getId()).orElseThrow();
        assertEquals("bananaUpdate", updatedWord.getWord());
        assertEquals("bananUpdate", updatedWord.getTranslation());
        assertNull(updatedWord.getAudioFilePath());
        assertNull(updatedWord.getImageFilePath());
    }

    @Test
    void shouldUpdateWordWithFiles() throws Exception {
        MockMultipartFile audioFile = new MockMultipartFile(
                "audioFile", "audio.mp3", MediaType.MULTIPART_FORM_DATA_VALUE, "dummy audio content".getBytes());
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "image.jpg", MediaType.MULTIPART_FORM_DATA_VALUE, "dummy image content".getBytes());

        MockMultipartHttpServletRequestBuilder requestBuilder = (MockMultipartHttpServletRequestBuilder) multipart("/api/v1/word/update/" + word.getId())
                .file(audioFile)
                .file(imageFile)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .param("word", "bananaUpdateWithFiles")
                .param("translation", "bananUpdateWithFiles");

        mockMvc.perform(requestBuilder
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().string("Word updated successfully"));

        Word updatedWord = wordRepository.findById(word.getId()).orElseThrow();
        assertEquals("bananaUpdateWithFiles", updatedWord.getWord());
        assertEquals("bananUpdateWithFiles", updatedWord.getTranslation());
        assertNotNull(updatedWord.getAudioFilePath());
        assertNotNull(updatedWord.getImageFilePath());
    }

    @Test
    void shouldReturnErrorForNonExistentWord() throws Exception {
        MockMultipartHttpServletRequestBuilder requestBuilder = (MockMultipartHttpServletRequestBuilder) multipart("/api/v1/word/update/999")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .param("wordUpdateRequest.word", "banana")
                .param("wordUpdateRequest.translation", "banan");

        mockMvc.perform(requestBuilder
                        .cookie(new Cookie("accessToken", jwtToken))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Word not found"));
    }

    @Test
    void shouldReturnErrorForUnauthorizedUpdate() throws Exception {
        User newUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("testuser2@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        User savedUser = userRepository.save(newUser);
        String newToken = jwtService.generateToken(savedUser);

        MockMultipartHttpServletRequestBuilder requestBuilder = (MockMultipartHttpServletRequestBuilder) multipart("/api/v1/word/update/" + word.getId())
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .param("word", "banana")
                .param("translation", "banan");

        mockMvc.perform(requestBuilder
                        .cookie(new Cookie("accessToken", newToken))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You are not allowed to edit this word"));
    }

    @Test
    void shouldDeleteWordSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/word/delete/" + word.getId())
                        .cookie(new Cookie("accessToken", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Word deleted successfully"));

        assertFalse(wordRepository.existsById(word.getId()));
    }
}

