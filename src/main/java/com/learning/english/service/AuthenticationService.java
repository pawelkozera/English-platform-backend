package com.learning.english.service;

import com.learning.english.dao.JwtAuthenticationResponse;
import com.learning.english.dao.SignUpRequest;
import com.learning.english.dao.SigninRequest;
import org.springframework.http.ResponseEntity;

public interface AuthenticationService {
    ResponseEntity<?> signup(SignUpRequest request);

    ResponseEntity<?> signin(SigninRequest request);
}
