package com.learning.english;

import com.learning.english.utils.TokenCookies;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseCookie;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TokenCookiesTest {
    @Test
    void shouldCreateAccessTokenCookie() {
        String jwt = "jwt-token";

        ResponseCookie cookie = TokenCookies.createAccessTokenCookie(jwt);

        assertNotNull(cookie);
        assertEquals("accessToken", cookie.getName());
        assertEquals(jwt, cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals("Strict", cookie.getSameSite());
    }

    @Test
    void shouldCreateRefreshTokenCookie() {
        String refreshToken = "refresh-token";

        ResponseCookie cookie = TokenCookies.createRefreshTokenCookie(refreshToken);

        assertNotNull(cookie);
        assertEquals("refreshToken", cookie.getName());
        assertEquals(refreshToken, cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/api/v1/auth/", cookie.getPath());
        assertEquals("Strict", cookie.getSameSite());
    }

    @Test
    void shouldRemoveAccessTokenCookie() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        TokenCookies.removeAccessTokenCookie(response);

        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);

        verify(response).addCookie(sameCookie(accessTokenCookie));
    }

    @Test
    void shouldRemoveRefreshTokenCookie() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        TokenCookies.removeRefreshTokenCookie(response);

        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/api/v1/auth/");
        refreshTokenCookie.setMaxAge(0);

        verify(response).addCookie(sameCookie(refreshTokenCookie));
    }

    @Test
    void shouldExtractAccessTokenFromCookies() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie accessTokenCookie = new Cookie("accessToken", "jwt-token");
        when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});

        String accessToken = TokenCookies.extractAccessToken(request);

        assertEquals("jwt-token", accessToken);
    }

    @Test
    void shouldThrowExceptionWhenAccessTokenNotFound() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            TokenCookies.extractAccessToken(request);
        }, "Access token not found in cookies.");
    }

    @Test
    void shouldExtractRefreshTokenFromCookies() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie refreshTokenCookie = new Cookie("refreshToken", "refresh-token");
        when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie});

        String refreshToken = TokenCookies.extractRefreshToken(request);

        assertEquals("refresh-token", refreshToken);
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenNotFound() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            TokenCookies.extractRefreshToken(request);
        }, "Refresh token not found in cookies.");
    }

    private static Cookie sameCookie(Cookie expected) {
        return argThat(cookie ->
                expected.getName().equals(cookie.getName()) &&
                        ((expected.getValue() == null && cookie.getValue() == null) || (expected.getValue() != null && expected.getValue().equals(cookie.getValue()))) &&
                        expected.getMaxAge() == cookie.getMaxAge() &&
                        expected.getPath().equals(cookie.getPath()) &&
                        expected.getSecure() == cookie.getSecure() &&
                        expected.isHttpOnly() == cookie.isHttpOnly()
        );
    }
}
