package com.learning.english.repository;

import com.learning.english.models.RepetitionWord;
import com.learning.english.models.User;
import com.learning.english.models.Word;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RepetitionWordRepository extends JpaRepository<RepetitionWord, Integer> {
    Optional<RepetitionWord> findByRepetition_StudentAndWord_Id(User student, Integer wordId);
    boolean existsByRepetitionStudentAndWord(User student, Word word);
    long countByRepetitionStudentAndRepetitionGroupIdAndNextReviewDate(User student, Integer groupId, LocalDate nextReviewDate);

}
