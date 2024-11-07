package com.learning.english.repository;

import com.learning.english.models.SuspiciousActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuspiciousActivityRepository extends JpaRepository<SuspiciousActivity, Integer> {
}