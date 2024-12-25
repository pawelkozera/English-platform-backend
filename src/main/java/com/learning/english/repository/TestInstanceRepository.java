package com.learning.english.repository;

import com.learning.english.models.TestInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestInstanceRepository extends CrudRepository<TestInstance, Integer> {
    Page<TestInstance> findByGroupId(Integer groupId, Pageable pageable);
    List<TestInstance> findAllByGroup_Id(Integer groupId);

    boolean existsByTestTemplateIdAndGroupId(int testTemplateId, int groupId);
}
