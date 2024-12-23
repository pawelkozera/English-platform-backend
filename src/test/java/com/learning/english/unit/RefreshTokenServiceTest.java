package com.learning.english.unit;

import com.learning.english.models.*;
import com.learning.english.repository.*;
import com.learning.english.service.JwtService;
import com.learning.english.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setEmail("user@example.com");

        refreshToken = new RefreshToken();
        refreshToken.setUserInfo(user);
        refreshToken.setToken("valid-refresh-token");
        refreshToken.setExpiryDate(Instant.now().plusMillis(24 * 60 * 60 * 1000));
    }

    @Test
    void shouldCreateOrUpdateRefreshToken() {
        String username = "user@example.com";
        when(userRepository.findByEmail(username)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserInfo(user)).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        RefreshToken createdToken = refreshTokenService.createOrUpdateRefreshToken(username);

        assertNotNull(createdToken);
        assertNotNull(createdToken.getToken());
        assertTrue(createdToken.getExpiryDate().isAfter(Instant.now()));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void shouldUpdateExistingRefreshToken() {
        String username = "user@example.com";
        when(userRepository.findByEmail(username)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserInfo(user)).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        RefreshToken oldToken = refreshToken;
        RefreshToken updatedToken = refreshTokenService.createOrUpdateRefreshToken(username);

        assertNotNull(updatedToken);
        assertNotEquals(oldToken, updatedToken.getToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void shouldFindByToken() {
        String token = "valid-refresh-token";
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = refreshTokenService.findByToken(token);

        assertTrue(result.isPresent());
        assertEquals(refreshToken, result.get());
    }

    @Test
    void shouldThrowExceptionWhenTokenIsExpired() {
        refreshToken.setExpiryDate(Instant.now().minusMillis(1));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.verifyExpiration(refreshToken);
        });

        assertEquals(refreshToken.getToken() + " Refresh token is expired. Please make a new login..!", exception.getMessage());
        verify(refreshTokenRepository, times(1)).delete(refreshToken);
    }

    @Test
    void shouldReturnTokenWhenNotExpired() {
        refreshToken.setExpiryDate(Instant.now().plusMillis(24 * 60 * 60 * 1000));

        RefreshToken result = refreshTokenService.verifyExpiration(refreshToken);

        assertEquals(refreshToken, result);
    }
}
