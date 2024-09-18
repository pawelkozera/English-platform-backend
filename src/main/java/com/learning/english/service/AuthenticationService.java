package com.learning.english.service;

import com.learning.english.dto.SignUpRequest;
import com.learning.english.dto.SigninRequest;
import com.learning.english.models.Role;
import com.learning.english.models.User;
import com.learning.english.repository.UserRepository;
import com.learning.english.exception.UserAlreadyExistsException;
import com.learning.english.utils.TokenCookies;
import jakarta.servlet.http.HttpServletResponse;
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
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

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

        ResponseCookie accessTokenCookie = TokenCookies.createAccessTokenCookie(jwt);
        ResponseCookie refreshTokenCookie = TokenCookies.createRefreshTokenCookie(refreshToken);

        return ResponseEntity.ok()
                .header("Set-Cookie", accessTokenCookie.toString())
                .header("Set-Cookie", refreshTokenCookie.toString())
                .body("User registered successfully");
    }

    public ResponseEntity<?> signin(SigninRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        if (authentication.isAuthenticated()) {
            String refreshToken = refreshTokenService.createOrUpdateRefreshToken(request.getEmail()).getToken();
            User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                    () -> new IllegalArgumentException("Invalid email or password.")
            );
            String jwt = jwtService.generateToken(user);

            ResponseCookie accessTokenCookie = TokenCookies.createAccessTokenCookie(jwt);
            ResponseCookie refreshTokenCookie = TokenCookies.createRefreshTokenCookie(refreshToken);

            return ResponseEntity.ok()
                    .header("Set-Cookie", accessTokenCookie.toString())
                    .header("Set-Cookie", refreshTokenCookie.toString())
                    .body("User logged in successfully");
        }

        throw new UsernameNotFoundException("Invalid user request");
    }

    public ResponseEntity<?> logout(HttpServletResponse response) {
        TokenCookies.remove(response);

        return ResponseEntity.ok("Logged out successfully");
    }
}