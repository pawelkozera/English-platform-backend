package com.learning.english.service;

import com.learning.english.dto.RepetitionAddRequest;
import com.learning.english.models.Repetition;
import com.learning.english.models.User;
import com.learning.english.models.Word;
import com.learning.english.repository.RepetitionRepository;
import com.learning.english.repository.WordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepetitionService {
    private final RepetitionRepository repetitionRepository;
    private final WordRepository wordRepository;

    @Transactional
    public void addRepetition(RepetitionAddRequest repetitionAddRequest, User user) {
        Word word = wordRepository.findById(repetitionAddRequest.getWordId())
                .orElseThrow(() -> new IllegalArgumentException("Word not found"));

        boolean repetitionExists = repetitionRepository.existsByStudentAndWordsContaining(user, word);
        if (repetitionExists) {
            throw new IllegalArgumentException("Repetition for this word already exists");
        }

        Repetition repetition = Repetition.builder()
                .student(user)
                .words(List.of(word))
                .eFactor(2.5)
                .interval(1)
                .nextReviewDate(LocalDate.now().plusDays(1))
                .build();

        word.getRepetitions().add(repetition);

        repetitionRepository.save(repetition);
    }
}
