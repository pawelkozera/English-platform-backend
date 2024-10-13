package com.learning.english.repository;

import com.learning.english.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {
    Optional<Group> findByGroupCode(String groupCode);
    @Query("SELECT CASE WHEN COUNT(ug) > 0 THEN true ELSE false END " +
            "FROM UserGroup ug WHERE ug.group.id = :groupId AND ug.user.id = :userId")
    boolean existsByGroupIdAndUserId(@Param("groupId") Integer groupId, @Param("userId") Integer userId);
}
