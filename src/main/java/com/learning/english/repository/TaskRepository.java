package com.learning.english.repository;

import com.learning.english.models.Task;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TaskRepository extends CrudRepository<Task, Integer> {
    List<Task> findByLessonId(Integer lessonId);
}