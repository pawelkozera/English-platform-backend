package com.learning.english.controllers;

import com.learning.english.dao.UserProfileResponse;
import com.learning.english.service.JwtService;
import com.learning.english.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping("/profile")
    public UserProfileResponse getProfile(HttpServletRequest request) {
        String jwtToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }
        if (jwtToken != null) {
            String email = jwtService.extractUserName(jwtToken);

            return userService.getUserProfileByEmail(email);
        } else {
            throw new RuntimeException("Token not found");
        }
    }
}
