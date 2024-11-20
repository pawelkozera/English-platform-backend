package com.learning.english.service;

import com.learning.english.dto.RepetitionAddRequest;
import com.learning.english.models.Repetition;
import com.learning.english.models.RepetitionWord;
import com.learning.english.models.User;
import com.learning.english.models.Word;
import com.learning.english.repository.RepetitionRepository;
import com.learning.english.repository.RepetitionWordRepository;
import com.learning.english.repository.WordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RepetitionService {
    private final RepetitionRepository repetitionRepository;
    private final WordRepository wordRepository;
    private final RepetitionWordRepository repetitionWordRepository;

    @Transactional
    public void addRepetition(RepetitionAddRequest repetitionAddRequest, User user) {
        Word word = wordRepository.findById(repetitionAddRequest.getWordId())
                .orElseThrow(() -> new IllegalArgumentException("Word not found"));

        boolean repetitionExists = repetitionWordRepository.existsByRepetitionStudentAndWord(user, word);
        if (repetitionExists) {
            throw new IllegalArgumentException("Repetition for this word already exists");
        }

        Repetition repetition = Repetition.builder()
                .student(user)
                .build();

        repetitionRepository.save(repetition);

        RepetitionWord repetitionWord = RepetitionWord.builder()
                .repetition(repetition)
                .word(word)
                .eFactor(2.5)
                .interval(1)
                .nextReviewDate(LocalDate.now().plusDays(1))
                .build();

        repetitionWordRepository.save(repetitionWord);
    }

    @Transactional
    public void removeRepetition(Integer wordId, User user) {
        RepetitionWord repetitionWord = repetitionWordRepository.findByRepetition_StudentAndWord_Id(user, wordId)
                .orElseThrow(() -> new IllegalArgumentException("Repetition word not found"));

        repetitionWordRepository.delete(repetitionWord);

        if (repetitionWord.getRepetition().getRepetitionWords().isEmpty()) {
            repetitionRepository.delete(repetitionWord.getRepetition());
        }
    }
}
