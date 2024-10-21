package com.learning.english.repository;

import com.learning.english.models.Task;
import com.learning.english.models.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TaskRepository extends CrudRepository<Task, Integer> {
    List<Task> findByLessonId(Integer lessonId);
    Page<Task> findByOwner(User owner, Pageable pageable);
}