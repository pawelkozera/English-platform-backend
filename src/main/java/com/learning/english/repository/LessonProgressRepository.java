package com.learning.english.repository;

import com.learning.english.models.Lesson;
import com.learning.english.models.LessonProgress;
import com.learning.english.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
    Optional<LessonProgress> findByUserAndLesson(User user, Lesson lesson);
}

