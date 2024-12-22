package com.learning.english;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learning.english.dto.WordAddRequest;
import com.learning.english.dto.WordResponse;
import com.learning.english.models.RepetitionWord;
import com.learning.english.models.Task;
import com.learning.english.models.User;
import com.learning.english.models.Word;
import com.learning.english.repository.RepetitionWordRepository;
import com.learning.english.repository.WordRepository;
import com.learning.english.service.WordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class WordServiceTest {
    @Mock
    private WordRepository wordRepository;

    @Mock
    private RepetitionWordRepository repetitionWordRepository;

    @InjectMocks
    private WordService wordService;

    @Test
    void shouldReturnWordsOwnedByUser() {
        User user = new User();
        user.setId(1);

        Word word1 = new Word();
        word1.setId(1L);
        word1.setWord("apple");
        word1.setTranslation("jabłko");
        word1.setAudioFilePath("/audio/apple.mp3");
        word1.setImageFilePath("/images/apple.jpg");

        Word word2 = new Word();
        word2.setId(2L);
        word2.setWord("banana");
        word2.setTranslation("banan");
        word2.setAudioFilePath("/audio/banana.mp3");
        word2.setImageFilePath("/images/banana.jpg");

        List<Word> words = Arrays.asList(word1, word2);
        Pageable pageable = PageRequest.of(0, 2);
        Page<Word> wordPage = new PageImpl<>(words, pageable, words.size());

        when(wordRepository.findAllByCreatedBy(user, pageable)).thenReturn(wordPage);

        Page<WordResponse> wordResponses = wordService.getWordsOwnedByUser(user, 0, 2);

        assertNotNull(wordResponses);
        assertEquals(2, wordResponses.getContent().size());
        assertEquals("apple", wordResponses.getContent().get(0).getWord());
        assertEquals("jabłko", wordResponses.getContent().get(0).getTranslation());
        assertEquals("/audio/apple.mp3", wordResponses.getContent().get(0).getAudioFilePath());
        assertEquals("/images/apple.jpg", wordResponses.getContent().get(0).getImageFilePath());
    }

    @Test
    void shouldReturnEmptyPageWhenNoWords() {
        User user = new User();
        user.setId(1);

        Pageable pageable = PageRequest.of(0, 2);
        Page<Word> wordPage = Page.empty(pageable);

        when(wordRepository.findAllByCreatedBy(user, pageable)).thenReturn(wordPage);

        Page<WordResponse> wordResponses = wordService.getWordsOwnedByUser(user, 0, 2);

        assertNotNull(wordResponses);
        assertEquals(0, wordResponses.getContent().size());
    }

    @Test
    void shouldHandleRepositoryException() {
        User user = new User();
        user.setId(1);

        Pageable pageable = PageRequest.of(0, 2);
        when(wordRepository.findAllByCreatedBy(user, pageable)).thenThrow(new DataIntegrityViolationException("Data integrity violation"));

        assertThrows(DataIntegrityViolationException.class, () -> {
            wordService.getWordsOwnedByUser(user, 0, 2);
        });
    }

    @Test
    void shouldAddWordSuccessfully() {
        User user = new User();
        user.setId(1);

        WordAddRequest wordAddRequest = new WordAddRequest();
        wordAddRequest.setWord("apple");
        wordAddRequest.setTranslation("jabłko");
        wordAddRequest.setAudioFilePath("/audio/apple.mp3");
        wordAddRequest.setImageFilePath("/images/apple.jpg");

        Word word = Word.builder()
                .word("apple")
                .translation("jabłko")
                .audioFilePath("/audio/apple.mp3")
                .imageFilePath("/images/apple.jpg")
                .createdBy(user)
                .build();

        when(wordRepository.save(any(Word.class))).thenReturn(word);

        wordService.addWord(wordAddRequest, user);

        verify(wordRepository, times(1)).save(any(Word.class));
    }

    @Test
    void shouldAddWordWithOptionalFields() {
        User user = new User();
        user.setId(1);

        WordAddRequest wordAddRequest = new WordAddRequest();
        wordAddRequest.setWord("banana");
        wordAddRequest.setTranslation("banan");
        wordAddRequest.setAudioFilePath("/audio/banana.mp3");
        wordAddRequest.setImageFilePath("/images/banana.jpg");

        Word word = Word.builder()
                .word("banana")
                .translation("banan")
                .audioFilePath("/audio/banana.mp3")
                .imageFilePath("/images/banana.jpg")
                .createdBy(user)
                .build();

        when(wordRepository.save(any(Word.class))).thenReturn(word);

        wordService.addWord(wordAddRequest, user);

        verify(wordRepository, times(1)).save(any(Word.class));
    }

    @Test
    void shouldThrowExceptionIfWordNotFound() {
        Long wordId = 999L;
        User user = new User();
        user.setId(1);

        WordAddRequest wordUpdateRequest = new WordAddRequest();
        wordUpdateRequest.setWord("Updated Word");

        assertThrows(IllegalArgumentException.class, () -> {
            wordService.updateWord(wordId, wordUpdateRequest, user);
        });
    }

    @Test
    void shouldThrowExceptionIfUserNotAuthorized() {
        Long wordId = 1L;
        User unauthorizedUser = new User();
        unauthorizedUser.setId(2);

        WordAddRequest wordUpdateRequest = new WordAddRequest();
        wordUpdateRequest.setWord("Updated Word");

        assertThrows(IllegalArgumentException.class, () -> {
            wordService.updateWord(wordId, wordUpdateRequest, unauthorizedUser);
        });
    }

    @Test
    void shouldUpdateWordSuccessfully() {
        Long wordId = 1L;
        User user = new User();
        user.setId(1);

        Word existingWord = new Word();
        existingWord.setId(wordId);
        existingWord.setWord("Original Word");
        existingWord.setTranslation("Original Translation");
        existingWord.setCreatedBy(user);

        when(wordRepository.findById(wordId)).thenReturn(Optional.of(existingWord));

        WordAddRequest wordUpdateRequest = new WordAddRequest();
        wordUpdateRequest.setWord("Updated Word");
        wordUpdateRequest.setTranslation("Updated Translation");

        wordService.updateWord(wordId, wordUpdateRequest, user);

        assertEquals("Updated Word", existingWord.getWord());
        assertEquals("Updated Translation", existingWord.getTranslation());

        verify(wordRepository).save(existingWord);
    }

    @Test
    void shouldThrowExceptionIfWordNotFoundWhenDeleting() {
        Long wordId = 999L;
        User user = new User();
        user.setId(1);

        assertThrows(RuntimeException.class, () -> {
            wordService.deleteWord(wordId, user);
        });
    }

    @Test
    void shouldThrowExceptionIfUserNotAuthorizedWhenDeleting() {
        Long wordId = 1L;
        User unauthorizedUser = new User();
        unauthorizedUser.setId(2);

        Word word = new Word();
        word.setId(wordId);
        word.setCreatedBy(new User());

        when(wordRepository.findById(wordId)).thenReturn(Optional.of(word));

        assertThrows(RuntimeException.class, () -> {
            wordService.deleteWord(wordId, unauthorizedUser);
        });
    }

    @Test
    void shouldThrowExceptionIfWordUsedInTasks() {
        Long wordId = 1L;
        User user = new User();
        user.setId(1);

        Word word = new Word();
        word.setId(wordId);
        word.setCreatedBy(user);
        word.setTasks(List.of());

        when(wordRepository.findById(wordId)).thenReturn(Optional.of(word));

        word.setTasks(List.of(new Task()));

        assertThrows(RuntimeException.class, () -> {
            wordService.deleteWord(wordId, user);
        });
    }

    @Test
    void shouldDeleteWordFromRepetitions() {
        Long wordId = 1L;
        User user = new User();
        user.setId(1);

        Word word = new Word();
        word.setId(wordId);
        word.setCreatedBy(user);
        word.setTasks(List.of());
        word.setRepetitionWords(List.of(new RepetitionWord()));

        when(wordRepository.findById(wordId)).thenReturn(Optional.of(word));

        doNothing().when(repetitionWordRepository).deleteAllByWord(word);
        doNothing().when(wordRepository).delete(word);

        wordService.deleteWord(wordId, user);

        verify(repetitionWordRepository).deleteAllByWord(word);

        verify(wordRepository).delete(word);
    }

    @Test
    void shouldDeleteWordSuccessfully() {
        Long wordId = 1L;
        User user = new User();
        user.setId(1);

        Word word = new Word();
        word.setId(wordId);
        word.setCreatedBy(user);
        word.setTasks(List.of());
        word.setRepetitionWords(List.of());

        when(wordRepository.findById(wordId)).thenReturn(Optional.of(word));

        wordService.deleteWord(wordId, user);

        verify(wordRepository).delete(word);
    }
}