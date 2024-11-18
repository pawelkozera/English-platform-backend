package com.learning.english.repository;

import com.learning.english.models.Repetition;
import com.learning.english.models.User;
import com.learning.english.models.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepetitionRepository extends JpaRepository<Repetition, Integer> {
    boolean existsByStudentAndWordsContaining(User student, Word word);
}
