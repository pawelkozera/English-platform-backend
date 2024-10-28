package com.learning.english.repository;

import com.learning.english.models.Group;
import com.learning.english.models.User;
import com.learning.english.models.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Integer> {
    List<UserGroup> findByUser(User user);
    List<UserGroup> findAllByUser(User user);
    Optional<UserGroup> findByUserAndGroup(User user, Group group);
    Optional<UserGroup> findByUserAndGroupId(User user, Integer groupId);
    boolean existsByUserAndGroupAndIsOwnerTrue(User user, Group group);
    boolean existsByUserAndGroup(User user, Group group);
}