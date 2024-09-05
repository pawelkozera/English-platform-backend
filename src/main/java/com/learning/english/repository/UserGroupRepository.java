package com.learning.english.repository;

import com.learning.english.models.User;
import com.learning.english.models.UserGroup;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGroupRepository extends CrudRepository<UserGroup, Integer> {
    List<UserGroup> findByUser(User user);
    List<UserGroup> findByUserAndIsOwnerTrue(User user);
}