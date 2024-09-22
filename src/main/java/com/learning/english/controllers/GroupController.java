package com.learning.english.controllers;

import com.learning.english.dto.GroupCreateRequest;
import com.learning.english.dto.GroupJoinRequest;
import com.learning.english.models.User;
import com.learning.english.service.GroupService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
