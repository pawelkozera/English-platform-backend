package com.learning.english.repository;

import com.learning.english.models.User;
import com.learning.english.models.Word;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordRepository extends CrudRepository<Word, Integer> {
    List<Word> findAllByCreatedBy(User user);
}