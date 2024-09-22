package com.learning.english.utils;

import com.learning.english.models.User;
import com.learning.english.service.JwtService;
import com.learning.english.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUtil {
    private final JwtService jwtService;
    private final UserService userService;

    public User getAuthenticatedUser(HttpServletRequest request) {
        String jwtToken = TokenCookies.extractAccessToken(request);
        String email = jwtService.extractUserName(jwtToken);
        return userService.findByEmail(email);
    }
}
