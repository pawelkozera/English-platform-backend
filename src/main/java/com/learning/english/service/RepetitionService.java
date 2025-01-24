package com.learning.english.service;

import com.learning.english.dto.RepetitionAddRequest;
import com.learning.english.dto.RepetitionDisplayRequest;
import com.learning.english.dto.RepetitionDisplayResponse;
import com.learning.english.dto.RepetitionUpdateRequest;
import com.learning.english.models.*;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.RepetitionRepository;
import com.learning.english.repository.RepetitionWordRepository;
import com.learning.english.repository.WordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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

    public boolean isWordInRepetitions(Long wordId, User user) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Word not found"));

        return repetitionWordRepository.existsByRepetitionStudentAndWord(user, word);
    }

    public long countRepetitionsForTodayByGroup(User user, Integer groupId) {
        LocalDate today = LocalDate.now();
        return repetitionWordRepository.countByRepetitionStudentAndRepetitionGroupIdAndNextReviewDateLessThanEqual(user, groupId, today);
    }

    public List<RepetitionDisplayResponse> getRepetitionWords(User user, RepetitionDisplayRequest repetitionDisplayRequest) {
        Pageable pageable = PageRequest.of(0, repetitionDisplayRequest.getLimit());

        List<RepetitionWord> repetitions;
        if (repetitionDisplayRequest.getAnsweredWordIds() != null && !repetitionDisplayRequest.getAnsweredWordIds().isEmpty()) {
            repetitions = repetitionWordRepository.findDueRepetitionsExcludingAnswered(user.getId(), repetitionDisplayRequest.getGroupId(), repetitionDisplayRequest.getAnsweredWordIds(), pageable);
        } else {
            repetitions = repetitionWordRepository.findDueRepetitions(user.getId(), repetitionDisplayRequest.getGroupId(), pageable);
        }

        return repetitions.stream()
                .map(repetition -> RepetitionDisplayResponse.builder()
                        .repetitionWordId(repetition.getId())
                        .word(repetition.getWord().getWord())
                        .translation(repetition.getWord().getTranslation())
                        .audioFilePath(repetition.getWord().getAudioFilePath())
                        .imageFilePath(repetition.getWord().getImageFilePath())
                        .build())
                .toList();
    }

    @Transactional
    public void updateRepetition(RepetitionUpdateRequest repetitionUpdateRequest, User user) {
        RepetitionWord repetitionWord = repetitionWordRepository.findById(repetitionUpdateRequest.getRepetitionWordId())
                .orElseThrow(() -> new IllegalArgumentException("Repetition word not found"));

        Repetition repetition = repetitionWord.getRepetition();

        if (!repetition.getStudent().equals(user)) {
            throw new IllegalArgumentException("User does not have permission to update this repetition word");
        }

        int grade = repetitionUpdateRequest.getGrade();
        if (grade < 1 || grade > 3) {
            throw new IllegalArgumentException("Invalid grade");
        }

        double newEFactor = repetitionWord.getEFactor() + (0.1 - (3 - grade) * (0.08 + (3 - grade) * 0.02));
        repetitionWord.setEFactor(Math.max(1.3, newEFactor));

        if (grade == 1) {
            repetitionWord.setInterval(1);
        } else if (grade == 2) {
            repetitionWord.setInterval(Math.max(1, (int) (repetitionWord.getInterval() * 0.5)));
        } else {
            repetitionWord.setInterval(repetitionWord.getInterval() == 0 ? 1 : (int)
                    (repetitionWord.getInterval() * repetitionWord.getEFactor()));
        }

        repetitionWord.setNextReviewDate(LocalDate.now().plusDays(repetitionWord.getInterval()));

        repetitionWordRepository.save(repetitionWord);
    }
}
