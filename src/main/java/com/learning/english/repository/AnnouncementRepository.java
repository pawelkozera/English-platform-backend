package com.learning.english.repository;

import com.learning.english.models.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {
    Page<Announcement> findByGroupId(Integer groupId, Pageable pageable);
}
