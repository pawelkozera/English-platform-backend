package com.learning.english.repository;

import com.learning.english.models.Announcement;
import com.learning.english.models.User;
import com.learning.english.models.UserAnnouncement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAnnouncementRepository extends CrudRepository<UserAnnouncement, Integer> {
    Optional<UserAnnouncement> findByUserAndAnnouncement(User user, Announcement announcement);
    @Query("SELECT COUNT(a) FROM Announcement a " +
            "WHERE a.group.id = :groupId AND NOT EXISTS (" +
            "  SELECT ua FROM UserAnnouncement ua " +
            "  WHERE ua.announcement.id = a.id AND ua.user.id = :userId AND ua.seen = true" +
            ")")
    Long countUnseenAnnouncementsByUserAndGroup(@Param("userId") Integer userId, @Param("groupId") Integer groupId);
}
