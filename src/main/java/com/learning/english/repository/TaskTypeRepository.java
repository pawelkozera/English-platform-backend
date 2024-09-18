package com.learning.english.repository;

import com.learning.english.models.TaskType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskTypeRepository extends CrudRepository<TaskType, Integer> {
    Optional<TaskType> findByTypeName(String typeName);
}
