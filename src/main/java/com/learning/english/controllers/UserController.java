package com.learning.english.controllers;

import com.learning.english.dto.GroupResponse;
import com.learning.english.dto.UserProfileResponse;
import com.learning.english.service.JwtService;
import com.learning.english.service.UserService;
import com.learning.english.utils.TokenCookies;
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
    private final JwtService jwtService;

    @GetMapping("/profile")
    public UserProfileResponse getProfile(HttpServletRequest request) {
        String jwtToken = TokenCookies.extractAccessToken(request);
        String email = jwtService.extractUserName(jwtToken);
        return userService.getUserProfileByEmail(email);
    }

    @GetMapping("/allGroups")
    public List<GroupResponse> getAllUserGroups(HttpServletRequest request) {
        String jwtToken = TokenCookies.extractAccessToken(request);
        String email = jwtService.extractUserName(jwtToken);
        return userService.getAllUserGroups(email);
    }
}
