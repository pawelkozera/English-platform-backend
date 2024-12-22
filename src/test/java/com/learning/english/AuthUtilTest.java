package com.learning.english;

import com.learning.english.models.User;
import com.learning.english.service.JwtService;
import com.learning.english.service.UserService;
import com.learning.english.utils.AuthUtil;
import com.learning.english.utils.TokenCookies;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthUtilTest {
    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthUtil authUtil;

    @Test
    void shouldReturnAccessTokenFromCookies() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie accessTokenCookie = new Cookie("accessToken", "valid-jwt-token");
        when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});

        String accessToken = TokenCookies.extractAccessToken(request);

        assertEquals("valid-jwt-token", accessToken);
    }

    @Test
    void shouldThrowExceptionWhenTokenIsMissingOrInvalid() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            authUtil.getAuthenticatedUser(request);
        }, "Access token not found in cookies.");
    }

    @Test
    void shouldReturnNullIfUserNotFound() {
        String jwtToken = "valid-jwt-token";
        String email = "testuser@example.com";

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("accessToken", jwtToken)});
        when(jwtService.extractUserName(jwtToken)).thenReturn(email);
        when(userService.findByEmail(email)).thenReturn(null);

        User result = authUtil.getAuthenticatedUser(request);

        assertNull(result);
    }

    @Test
    void shouldThrowExceptionIfJwtServiceFails() {
        String jwtToken = "invalid-jwt-token";
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("accessToken", jwtToken)});
        when(jwtService.extractUserName(jwtToken)).thenThrow(new RuntimeException("JWT parsing failed"));

        assertThrows(RuntimeException.class, () -> {
            authUtil.getAuthenticatedUser(request);
        }, "JWT parsing failed.");
    }
}
