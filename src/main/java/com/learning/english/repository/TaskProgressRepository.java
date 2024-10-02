package com.learning.english.repository;

import com.learning.english.models.LessonProgress;
import com.learning.english.models.TaskProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskProgressRepository extends JpaRepository<TaskProgress, Integer> {
    TaskProgress findByLessonProgressAndTaskId(LessonProgress lessonProgress, Integer taskId);
}
