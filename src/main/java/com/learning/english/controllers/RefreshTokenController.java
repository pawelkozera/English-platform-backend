package com.learning.english.controllers;

import com.learning.english.dao.JwtAuthenticationResponse;
import com.learning.english.models.RefreshToken;
import com.learning.english.service.JwtService;
import com.learning.english.service.RefreshTokenService;
import com.learning.english.utils.TokenCookies;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RefreshTokenController {
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @PostMapping("/refreshToken")
    public JwtAuthenticationResponse refreshToken(HttpServletRequest request) {
        String refreshToken = TokenCookies.extractRefreshToken(request);

        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserInfo)
                .map(userInfo -> {
                    String accessToken = jwtService.generateToken(userInfo);
                    return JwtAuthenticationResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken).build();
                }).orElseThrow(() -> new RuntimeException("Refresh Token is not in DB."));
    }
}