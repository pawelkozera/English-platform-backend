package com.learning.english.controllers;

import com.learning.english.dto.GroupAddLessonsRequest;
import com.learning.english.dto.GroupCreateRequest;
import com.learning.english.dto.GroupJoinRequest;
import com.learning.english.models.User;
import com.learning.english.service.GroupService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;
    private final AuthUtil authUtil;

    @PostMapping("/createGroup")
    public ResponseEntity<String> createGroup(HttpServletRequest request, @RequestBody GroupCreateRequest groupCreateRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        groupService.createGroup(groupCreateRequest, user);
        return ResponseEntity.ok("Group created successfully");
    }

    @PostMapping("/join")
    public ResponseEntity<String> joinGroup(HttpServletRequest request, @RequestBody GroupJoinRequest groupRequest) {
        User user = authUtil.getAuthenticatedUser(request);

        try {
            groupService.joinGroup(user, groupRequest);
            return ResponseEntity.ok("User successfully joined the group.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PutMapping("/{groupId}/addLessons")
    public ResponseEntity<String> addLessonsToGroup(
            HttpServletRequest request,
            @PathVariable Integer groupId,
            @RequestBody GroupAddLessonsRequest requestBody
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        boolean isAssigned = groupService.addLessonsToGroup(user, groupId, requestBody.getLessonIds());

        if (isAssigned) {
            return ResponseEntity.ok("Lessons successfully added to group.");
        } else {
            return ResponseEntity.status(404).body("Group not found or user is not authorized to add lessons to this group.");
        }
    }
}
