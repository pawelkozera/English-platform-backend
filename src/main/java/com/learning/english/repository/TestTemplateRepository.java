package com.learning.english.repository;

import com.learning.english.models.TestTemplate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestTemplateRepository extends CrudRepository<TestTemplate, Integer> {
}
