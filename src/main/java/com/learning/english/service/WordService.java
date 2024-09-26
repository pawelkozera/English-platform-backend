package com.learning.english.service;

import com.learning.english.dto.WordAddRequest;
import com.learning.english.dto.WordResponse;
import com.learning.english.models.User;
import com.learning.english.models.Word;
import com.learning.english.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WordService {
    private final WordRepository wordRepository;

    public List<WordResponse> getWordsOwnedByUser(User user) {
        return wordRepository.findAllByCreatedBy(user).stream()
                .map(word -> WordResponse.builder()
                        .id(word.getId())
                        .word(word.getWord())
                        .translation(word.getTranslation())
                        .audioFilePath(word.getAudioFilePath())
                        .imageFilePath(word.getImageFilePath())
                        .build())
                .collect(Collectors.toList());
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
