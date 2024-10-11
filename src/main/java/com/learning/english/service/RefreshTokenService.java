package com.learning.english.service;

import com.learning.english.dto.SigninRequest;
import com.learning.english.models.RefreshToken;
import com.learning.english.models.User;
import com.learning.english.repository.RefreshTokenRepository;
import com.learning.english.repository.UserRepository;
import com.learning.english.utils.TokenCookies;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public ResponseEntity<String> refreshToken(HttpServletRequest request) {
        String refreshToken = TokenCookies.extractRefreshToken(request);
        RefreshToken validRefreshToken = verifyExpiration(findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token is not in DB.")));

        User userInfo = validRefreshToken.getUserInfo();
        String accessToken = jwtService.generateToken(userInfo);
        ResponseCookie accessTokenCookie = TokenCookies.createAccessTokenCookie(accessToken);

        return ResponseEntity.ok()
                .header("Set-Cookie", accessTokenCookie.toString())
                .body("Tokens refreshed successfully");
    }

    public RefreshToken createOrUpdateRefreshToken(String username) {
        Optional<User> userOptional = userRepository.findByEmail(username);
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + username);
        }

        User userInfo = userOptional.get();
        Optional<RefreshToken> existingTokenOptional = refreshTokenRepository.findByUserInfo(userInfo);

        RefreshToken refreshToken;
        if (existingTokenOptional.isPresent()) {
            refreshToken = existingTokenOptional.get();
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setExpiryDate(Instant.now().plusMillis(24 * 60 * 60 * 1000));
        } else {
            refreshToken = RefreshToken.builder()
                    .userInfo(userInfo)
                    .token(UUID.randomUUID().toString())
                    .expiryDate(Instant.now().plusMillis(24 * 60 * 60 * 1000))
                    .build();
        }

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token){
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token){
        if(token.getExpiryDate().compareTo(Instant.now())<0){
            refreshTokenRepository.delete(token);
            throw new RuntimeException(token.getToken() + " Refresh token is expired. Please make a new login..!");
        }

        return token;
    }
}
