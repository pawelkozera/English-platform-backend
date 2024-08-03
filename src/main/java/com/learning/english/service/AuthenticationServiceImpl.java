package com.learning.english.service;

import com.learning.english.dao.JwtAuthenticationResponse;
import com.learning.english.dao.SignUpRequest;
import com.learning.english.dao.SigninRequest;
import com.learning.english.models.Role;
import com.learning.english.models.User;
import com.learning.english.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Override
    public JwtAuthenticationResponse signup(SignUpRequest request) {
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER).build();
        userRepository.save(user);

        String jwt = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createOrUpdateRefreshToken(user.getEmail()).getToken();

        return JwtAuthenticationResponse.builder().accessToken(jwt).refreshToken(refreshToken).build();
    }

    @Override
    public JwtAuthenticationResponse signin(SigninRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        if (authentication.isAuthenticated()) {
            String refreshToken = refreshTokenService.createOrUpdateRefreshToken(request.getEmail()).getToken();
            User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                    () -> new IllegalArgumentException("Invalid email or password.")
            );
            String jwt = jwtService.generateToken(user);

            return JwtAuthenticationResponse.builder().accessToken(jwt).refreshToken(refreshToken).build();
        }

        throw new UsernameNotFoundException("Invalid user request");
    }
}