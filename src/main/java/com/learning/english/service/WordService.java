package com.learning.english.service;

import com.learning.english.dto.WordAddRequest;
import com.learning.english.dto.WordResponse;
import com.learning.english.models.User;
import com.learning.english.models.Word;
import com.learning.english.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WordService {
    private final WordRepository wordRepository;

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
}
