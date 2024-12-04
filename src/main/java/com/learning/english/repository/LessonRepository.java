package com.learning.english.repository;

import com.learning.english.models.Group;
import com.learning.english.models.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    Page<Lesson> findAllByGroupsContaining(Group group, Pageable pageable);

    @Query("SELECT l FROM Lesson l WHERE l.id NOT IN " +
            "(SELECT lg.id FROM Group g JOIN g.lessons lg WHERE g.id = :groupId)")
    Page<Lesson> findLessonsNotAssignedToGroup(@Param("groupId") Integer groupId, Pageable pageable);
}
