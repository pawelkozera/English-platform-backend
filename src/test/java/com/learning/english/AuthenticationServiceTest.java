package com.learning.english;

import com.learning.english.dto.SignUpRequest;
import com.learning.english.dto.SigninRequest;
import com.learning.english.exception.UserAlreadyExistsException;
import com.learning.english.models.RefreshToken;
import com.learning.english.models.Role;
import com.learning.english.models.User;
import com.learning.english.repository.RefreshTokenRepository;
import com.learning.english.repository.UserRepository;
import com.learning.english.service.AuthenticationService;
import com.learning.english.service.JwtService;
import com.learning.english.service.RefreshTokenService;
import com.learning.english.utils.TokenCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    private AuthenticationService authenticationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private TokenCookies tokenCookies;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authenticationService = new AuthenticationService(userRepository, passwordEncoder, jwtService, refreshTokenService, authenticationManager, refreshTokenRepository);
    }

    @Test
    void testSignup_UserAlreadyExists() {
        SignUpRequest request = new SignUpRequest("John", "Doe", "john.doe@example.com", "password123");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new User()));

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            authenticationService.signup(request);
        });

        assertEquals("Please complete the registration by following the instructions sent to your email address.", exception.getMessage());
        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void testSignup_Success() {
        SignUpRequest request = new SignUpRequest("John", "Doe", "john.doe@example.com", "password123");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("jwtToken");
        when(refreshTokenService.createOrUpdateRefreshToken(savedUser.getEmail()))
                .thenReturn(RefreshToken.builder()
                        .token("refreshToken")
                        .build());

        ResponseCookie accessTokenCookie = TokenCookies.createAccessTokenCookie("jwtToken");
        ResponseCookie refreshTokenCookie = TokenCookies.createRefreshTokenCookie("refreshToken");

        ResponseEntity<?> response = authenticationService.signup(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("User registered successfully", response.getBody());
        assertTrue(Objects.requireNonNull(response.getHeaders().get("Set-Cookie")).contains(accessTokenCookie.toString()));
        assertTrue(Objects.requireNonNull(response.getHeaders().get("Set-Cookie")).contains(refreshTokenCookie.toString()));

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(jwtService, times(1)).generateToken(savedUser);
        verify(refreshTokenService, times(1)).createOrUpdateRefreshToken(savedUser.getEmail());
    }

    @Test
    void testSignin_Success() {
        SigninRequest request = new SigninRequest("john.doe@example.com", "password123");
        Authentication mockAuthentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refreshToken")
                .build();
        when(refreshTokenService.createOrUpdateRefreshToken(request.getEmail()))
                .thenReturn(refreshToken);

        User user = User.builder()
                .email(request.getEmail())
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .role(Role.USER)
                .build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwtToken");

        ResponseCookie accessTokenCookie = TokenCookies.createAccessTokenCookie("jwtToken");
        ResponseCookie refreshTokenCookie = TokenCookies.createRefreshTokenCookie("refreshToken");

        ResponseEntity<?> response = authenticationService.signin(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("User logged in successfully", response.getBody());
        assertTrue(response.getHeaders().get("Set-Cookie").contains(accessTokenCookie.toString()));
        assertTrue(response.getHeaders().get("Set-Cookie").contains(refreshTokenCookie.toString()));

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenService, times(1)).createOrUpdateRefreshToken(request.getEmail());
        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(jwtService, times(1)).generateToken(user);
    }

    @Test
    void testSignin_InvalidCredentials() {
        SigninRequest request = new SigninRequest("john.doe@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new IllegalArgumentException("Invalid email or password."));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.signin(request);
        });

        assertEquals("Invalid email or password.", exception.getMessage());

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(refreshTokenService, userRepository, jwtService);
    }

    @Test
    void testSignin_NotAuthenticated() {
        SigninRequest request = new SigninRequest("john.doe@example.com", "password123");
        Authentication mockAuthentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(false);

        Exception exception = assertThrows(UsernameNotFoundException.class, () -> {
            authenticationService.signin(request);
        });

        assertEquals("Invalid user request", exception.getMessage());

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(refreshTokenService, userRepository, jwtService);
    }
}

