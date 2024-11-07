package com.learning.english.repository;

import com.learning.english.models.TestHistory;
import com.learning.english.models.TestInstance;
import com.learning.english.models.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestHistoryRepository extends CrudRepository<TestHistory, Integer> {
    Optional<TestHistory> findByTestInstanceAndUser(TestInstance testInstance, User user);
    Optional<TestHistory> findByTestInstanceIdAndUserId(Integer testInstanceId, Integer userId);
}