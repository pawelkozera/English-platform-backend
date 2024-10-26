package com.learning.english.repository;

import com.learning.english.models.TestTemplate;
import com.learning.english.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestTemplateRepository extends CrudRepository<TestTemplate, Integer> {
    Page<TestTemplate> findByOwner(User owner, Pageable pageable);
}
