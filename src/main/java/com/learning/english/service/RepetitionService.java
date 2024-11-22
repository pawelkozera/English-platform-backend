package com.learning.english.service;

import com.learning.english.dto.RepetitionAddRequest;
import com.learning.english.models.*;
import com.learning.english.repository.GroupRepository;
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
    private final GroupRepository groupRepository;

    @Transactional
    public void addRepetition(RepetitionAddRequest repetitionAddRequest, User user) {
        Word word = wordRepository.findById(repetitionAddRequest.getWordId())
                .orElseThrow(() -> new IllegalArgumentException("Word not found"));

        Group group = groupRepository.findById(repetitionAddRequest.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        boolean userInGroup = group.getUserGroups().stream()
                .anyMatch(userGroup -> userGroup.getUser().equals(user));

        if (!userInGroup) {
            throw new IllegalArgumentException("User does not belong to the specified group");
        }

        boolean repetitionExists = repetitionWordRepository.existsByRepetitionStudentAndWord(user, word);
        if (repetitionExists) {
            throw new IllegalArgumentException("Repetition for this word already exists");
        }

        Repetition repetition = repetitionRepository.findByStudent(user)
                .orElseGet(() -> repetitionRepository.save(Repetition.builder().student(user).group(group).build()));

        RepetitionWord repetitionWord = RepetitionWord.builder()
                .repetition(repetition)
                .word(word)
                .eFactor(2.5)
                .interval(1)
                .nextReviewDate(LocalDate.now().plusDays(0))
                .build();

        repetitionWordRepository.save(repetitionWord);
    }

    @Transactional
    public void removeRepetition(Integer wordId, User user) {
        RepetitionWord repetitionWord = repetitionWordRepository.findByRepetition_StudentAndWord_Id(user, wordId)
                .orElseThrow(() -> new IllegalArgumentException("Repetition word not found"));

        Repetition repetition = repetitionWord.getRepetition();
        repetition.getRepetitionWords().remove(repetitionWord);

        repetitionWordRepository.delete(repetitionWord);

        if (repetition.getRepetitionWords().isEmpty()) {
            repetitionRepository.delete(repetition);
        }
    }

    public boolean isWordInRepetitions(Integer wordId, User user) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Word not found"));

        return repetitionWordRepository.existsByRepetitionStudentAndWord(user, word);
    }

    public long countRepetitionsForTodayByGroup(User user, Integer groupId) {
        LocalDate today = LocalDate.now();
        return repetitionWordRepository.countByRepetitionStudentAndRepetitionGroupIdAndNextReviewDate(user, groupId, today);
    }

}
