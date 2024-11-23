package com.learning.english.repository;

import com.learning.english.models.RepetitionWord;
import com.learning.english.models.User;
import com.learning.english.models.Word;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RepetitionWordRepository extends JpaRepository<RepetitionWord, Integer> {
    Optional<RepetitionWord> findByRepetition_StudentAndWord_Id(User student, Integer wordId);
    boolean existsByRepetitionStudentAndWord(User student, Word word);
    long countByRepetitionStudentAndRepetitionGroupIdAndNextReviewDateLessThanEqual(
            User student, Integer groupId, LocalDate nextReviewDate
    );

    @Query("SELECT rw FROM RepetitionWord rw " +
            "JOIN rw.repetition r " +
            "WHERE r.student.id = :userId " +
            "AND r.group.id = :groupId " +
            "AND rw.nextReviewDate <= CURRENT_DATE " +
            "ORDER BY rw.nextReviewDate ASC")
    List<RepetitionWord> findDueRepetitions(@Param("userId") Integer userId,
                                            @Param("groupId") Integer groupId,
                                            Pageable pageable);

    @Query("SELECT rw FROM RepetitionWord rw " +
            "JOIN rw.repetition r " +
            "WHERE r.student.id = :userId " +
            "AND r.group.id = :groupId " +
            "AND rw.nextReviewDate <= CURRENT_DATE " +
            "AND rw.id NOT IN :answeredWordIds " +
            "ORDER BY rw.nextReviewDate ASC")
    List<RepetitionWord> findDueRepetitionsExcludingAnswered(
            @Param("userId") Integer userId,
            @Param("groupId") Integer groupId,
            @Param("answeredWordIds") List<Integer> answeredWordIds,
            Pageable pageable);
}
