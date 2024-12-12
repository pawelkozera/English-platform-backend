package com.learning.english;

import com.learning.english.models.User;
import com.learning.english.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class JwtServiceTest {
    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jwtService = new JwtService("880787adb9e7418387f4f2104a0fff2c90b8228f6059b6439295e698f27c3a37");
    }

    @Test
    void shouldGenerateValidToken() {
        User user = new User();
        user.setEmail("test@example.com");

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals(user.getEmail(), jwtService.extractUserName(token));
    }

    @Test
    void shouldExtractUserNameFromToken() {
        User user = new User();
        user.setEmail("test@example.com");

        String token = jwtService.generateToken(user);

        String extractedUserName = jwtService.extractUserName(token);

        assertEquals("test@example.com", extractedUserName);
    }

    @Test
    void shouldReturnTrueForValidToken() {
        User user = new User();
        user.setEmail("test@example.com");

        String token = jwtService.generateToken(user);

        when(userDetails.getUsername()).thenReturn("test@example.com");

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        User user = new User();
        user.setEmail("test@example.com");

        String token = jwtService.generateToken(user);

        when(userDetails.getUsername()).thenReturn("other@example.com");

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void shouldReturnFalseForExpiredToken() throws Exception {
        String base64Key = "iAeHrbnnQYOH9PIRQKD/8sksIij2BZtkOSlemPJsOjc=";

        Field jwtSigningKeyField = JwtService.class.getDeclaredField("jwtSigningKey");
        jwtSigningKeyField.setAccessible(true);
        jwtSigningKeyField.set(jwtService, base64Key);

        Method getSigningKeyMethod = JwtService.class.getDeclaredMethod("getSigningKey");
        getSigningKeyMethod.setAccessible(true);
        Key signingKey = (Key) getSigningKeyMethod.invoke(jwtService);

        String token = Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject("test@example.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 5))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        when(userDetails.getUsername()).thenReturn("test@example.com");

        assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(token, userDetails));
    }
}