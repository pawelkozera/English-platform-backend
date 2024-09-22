package com.learning.english.service;

import com.learning.english.dto.WordAddRequest;
import com.learning.english.models.User;
import com.learning.english.models.Word;
import com.learning.english.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WordService {
    private final WordRepository wordRepository;

    public void addTask(WordAddRequest wordAddRequest, User user) {
        Word word = Word.builder()
                .word(wordAddRequest.getWord())
                .translation(wordAddRequest.getTranslation())
                .createdBy(user)
                .build();

        wordRepository.save(word);
    }
}
