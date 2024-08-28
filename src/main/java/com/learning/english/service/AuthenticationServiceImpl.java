package com.learning.english.service;

import com.learning.english.dao.JwtAuthenticationResponse;
import com.learning.english.dao.SignUpRequest;
import com.learning.english.dao.SigninRequest;
import com.learning.english.models.Role;
import com.learning.english.models.User;
import com.learning.english.repository.UserRepository;
import com.learning.english.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> signup(SignUpRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Please complete the registration by following the instructions sent to your email address.");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER).build();
        userRepository.save(user);

        String jwt = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createOrUpdateRefreshToken(user.getEmail()).getToken();

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", jwt)
                .httpOnly(true)
                .secure(false) // true when in production
                .path("/")
                .maxAge(60 * 24)
                .sameSite("Strict")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // true when in production
                .path("/api/v1/auth/refreshToken")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", accessTokenCookie.toString())
                .header("Set-Cookie", refreshTokenCookie.toString())
                .body("User registered successfully");
    }

    @Override
    public ResponseEntity<?> signin(SigninRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        if (authentication.isAuthenticated()) {
            String refreshToken = refreshTokenService.createOrUpdateRefreshToken(request.getEmail()).getToken();
            User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                    () -> new IllegalArgumentException("Invalid email or password.")
            );
            String jwt = jwtService.generateToken(user);

            ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", jwt)
                    .httpOnly(true)
                    .secure(false) // true when in production
                    .path("/")
                    .maxAge(60 * 60)
                    .sameSite("Strict")
                    .build();

            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(false) // true when in production
                    .path("/api/v1/auth/refreshToken")
                    .maxAge(7 * 24 * 60 * 60)
                    .sameSite("Strict")
                    .build();

            return ResponseEntity.ok()
                    .header("Set-Cookie", accessTokenCookie.toString())
                    .header("Set-Cookie", refreshTokenCookie.toString())
                    .body("User logged in successfully");
        }

        throw new UsernameNotFoundException("Invalid user request");
    }
}