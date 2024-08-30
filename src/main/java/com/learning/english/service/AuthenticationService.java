package com.learning.english.service;

import com.learning.english.dao.JwtAuthenticationResponse;
import com.learning.english.dao.SignUpRequest;
import com.learning.english.dao.SigninRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

public interface AuthenticationService {
    ResponseEntity<?> signup(SignUpRequest request);

    ResponseEntity<?> signin(SigninRequest request);

    ResponseEntity<?> logout(HttpServletResponse response);
}
