package com.learning.english.controllers;

import com.learning.english.models.User;
import com.learning.english.service.UserAnnouncementService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/userAnnouncement")
@RequiredArgsConstructor
public class UserAnnouncementController {
    private final AuthUtil authUtil;
    private final UserAnnouncementService userAnnouncementService;

    @GetMapping("/count/unseen/{groupId}")
    public ResponseEntity<Long> getCountUnseenAnnouncements(HttpServletRequest request, @PathVariable Integer groupId) {
        User user = authUtil.getAuthenticatedUser(request);
        long count = userAnnouncementService.countUnseenAnnouncements(user, groupId);
        return ResponseEntity.ok(count);
    }
}
