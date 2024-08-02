package com.learning.english.service;

import com.learning.english.dao.JwtAuthenticationResponse;
import com.learning.english.dao.SignUpRequest;
import com.learning.english.dao.SigninRequest;

public interface AuthenticationService {
    JwtAuthenticationResponse signup(SignUpRequest request);

    JwtAuthenticationResponse signin(SigninRequest request);
}
