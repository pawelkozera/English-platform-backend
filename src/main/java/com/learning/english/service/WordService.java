package com.learning.english.service;

import com.learning.english.dto.WordAddRequest;
import com.learning.english.dto.WordResponse;
import com.learning.english.models.User;
import com.learning.english.models.Word;
import com.learning.english.repository.RepetitionWordRepository;
import com.learning.english.repository.WordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WordService {
    private final WordRepository wordRepository;
    private final RepetitionWordRepository repetitionWordRepository;

    public Page<WordResponse> getWordsOwnedByUser(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return wordRepository.findAllByCreatedBy(user, pageable)
                .map(word -> WordResponse.builder()
                        .id(word.getId())
                        .word(word.getWord())
                        .translation(word.getTranslation())
                        .audioFilePath(word.getAudioFilePath())
                        .imageFilePath(word.getImageFilePath())
                        .build());
    }

    public void addWord(WordAddRequest wordAddRequest, User user) {
        Word word = Word.builder()
                .word(wordAddRequest.getWord())
                .translation(wordAddRequest.getTranslation())
                .audioFilePath(wordAddRequest.getAudioFilePath())
                .imageFilePath(wordAddRequest.getImageFilePath())
                .createdBy(user)
                .build();

        wordRepository.save(word);
    }

    public void updateWord(Long wordId, WordAddRequest wordUpdateRequest, User user) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Word not found"));

        if (!word.getCreatedBy().getId().equals(user.getId())) {
            throw new SecurityException("You are not allowed to edit this word");
        }

        word.setWord(wordUpdateRequest.getWord());
        word.setTranslation(wordUpdateRequest.getTranslation());
        word.setAudioFilePath(wordUpdateRequest.getAudioFilePath());
        word.setImageFilePath(wordUpdateRequest.getImageFilePath());

        wordRepository.save(word);
    }

    @Transactional
    public void deleteWord(Long wordId, User user) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("Word not found"));

        if (!word.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("You do not have permission to delete this word");
        }

        boolean isUsedInTasks = !word.getTasks().isEmpty();
        boolean isUsedInRepetitions = !word.getRepetitionWords().isEmpty();

        if (isUsedInTasks) {
            throw new RuntimeException("Cannot delete word because it is used in tasks. Remove the word from all tasks first.");
        }

        if (isUsedInRepetitions) {
            repetitionWordRepository.deleteAllByWord(word);
        }

        wordRepository.delete(word);
    }
}
