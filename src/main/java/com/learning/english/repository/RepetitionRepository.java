package com.learning.english.repository;

import com.learning.english.models.Repetition;
import com.learning.english.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepetitionRepository extends JpaRepository<Repetition, Integer> {
    Optional<Repetition> findByStudent(User student);
}
