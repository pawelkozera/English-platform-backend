package com.learning.english.repository;

import com.learning.english.models.TestInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestInstanceRepository extends CrudRepository<TestInstance, Integer> {
    Page<TestInstance> findByGroupId(Integer groupId, Pageable pageable);
}
