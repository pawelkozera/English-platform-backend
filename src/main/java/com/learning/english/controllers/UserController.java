package com.learning.english.controllers;

import com.learning.english.dto.GroupResponse;
import com.learning.english.dto.UserProfileResponse;
import com.learning.english.models.User;
import com.learning.english.service.UserService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthUtil authUtil;

    @GetMapping("/profile")
    public UserProfileResponse getProfile(HttpServletRequest request) {
        User user = authUtil.getAuthenticatedUser(request);
        return userService.getUserProfileByEmail(user);
    }

    @GetMapping("/allGroups")
    public List<GroupResponse> getAllUserGroups(HttpServletRequest request) {
        User user = authUtil.getAuthenticatedUser(request);
        return userService.getAllUserGroups(user);
    }
}
