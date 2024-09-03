package com.learning.english.service;

import com.learning.english.models.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String extractUserName(String token);

    String generateToken(User user);

    boolean isTokenValid(String token, UserDetails userDetails);
}