package com.learning.english.repository;

import com.learning.english.models.Group;
import com.learning.english.models.User;
import com.learning.english.models.UserGroup;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupRepository extends CrudRepository<UserGroup, Integer> {
    List<UserGroup> findByUser(User user);
    Optional<UserGroup> findByUserAndGroup(User user, Group group);
}