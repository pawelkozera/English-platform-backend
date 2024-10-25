package com.learning.english.repository;

import com.learning.english.models.TestInstance;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestInstanceRepository extends CrudRepository<TestInstance, Integer> {
}
