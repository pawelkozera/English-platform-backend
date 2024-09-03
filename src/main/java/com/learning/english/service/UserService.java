package com.learning.english.service;

import com.learning.english.dao.UserProfileResponse;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService {
    UserDetailsService userDetailsService();
    UserProfileResponse getUserProfileByEmail(String email);
}
