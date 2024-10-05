package com.learning.english.repository;

import com.learning.english.models.Group;
import com.learning.english.models.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends CrudRepository<Lesson, Integer> {
    List<Lesson> findAllByGroupsContaining(Group group);
    Page<Lesson> findAllByGroupsIn(List<Group> groups, Pageable pageable);
}
