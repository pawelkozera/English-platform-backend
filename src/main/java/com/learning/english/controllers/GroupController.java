package com.learning.english.controllers;

import com.learning.english.dao.GroupCreateRequest;
import com.learning.english.dao.GroupJoinRequest;
import com.learning.english.models.User;
import com.learning.english.service.GroupService;
import com.learning.english.service.JwtService;
import com.learning.english.service.UserService;
import com.learning.english.utils.TokenCookies;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupController {
    private final JwtService jwtService;
    private final UserService userService;
    private final GroupService groupService;

    @PostMapping("/createGroup")
    public ResponseEntity<String> createGroup(HttpServletRequest request, @RequestBody GroupCreateRequest groupCreateRequest) {
        String jwtToken = TokenCookies.extractAccessToken(request);
        String email = jwtService.extractUserName(jwtToken);

        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        groupService.createGroup(groupCreateRequest, user);

        return ResponseEntity.ok("Group created successfully");
    }

    @PostMapping("/join")
    public ResponseEntity<String> joinGroup(HttpServletRequest request, @RequestBody GroupJoinRequest groupRequest) {
        String jwtToken = TokenCookies.extractAccessToken(request);
        String email = jwtService.extractUserName(jwtToken);

        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        try {
            groupService.joinGroup(user, groupRequest);
            return ResponseEntity.ok("User successfully joined the group.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}
