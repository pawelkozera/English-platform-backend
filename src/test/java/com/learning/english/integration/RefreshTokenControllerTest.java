package com.learning.english.integration;

import com.learning.english.models.*;
import com.learning.english.repository.*;
import com.learning.english.service.JwtService;
import com.learning.english.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RefreshTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private static final String TEST_EMAIL = "testuser@example.com";
    private static final String TEST_PASSWORD = "password";
    private static final String TEST_FIRST_NAME = "Test";
    private static final String TEST_LAST_NAME = "User";

    private String refreshToken;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .email(TEST_EMAIL)
                .password("$2a$10$kvkhBZvBHKA/VRQlUpD42.a2WjFTkhU5Rvg/Xx4bgU3CMvTuF7ybi") // hashed "password"
                .role(com.learning.english.models.Role.USER)
                .build();
        userRepository.save(user);

        jwtToken = jwtService.generateToken(user);
        refreshToken = refreshTokenService.createOrUpdateRefreshToken(TEST_EMAIL).getToken();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refreshToken")
                        .cookie(new Cookie("accessToken", jwtToken), new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(content().string("Tokens refreshed successfully"));
    }

    @Test
    void shouldFailToRefreshTokenWhenTokenIsMissing() throws Exception {
        refreshTokenRepository.deleteAll();
        mockMvc.perform(post("/api/v1/auth/refreshToken")
                .cookie(new Cookie("accessToken", jwtToken), new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldFailToRefreshTokenWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refreshToken")
                        .cookie(new Cookie("accessToken", jwtToken), new Cookie("refreshToken", "invalid")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldFailToRefreshTokenWhenTokenIsExpired() throws Exception {
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken).orElseThrow();
        refreshTokenEntity.setExpiryDate(Instant.now().minusSeconds(1));
        refreshTokenRepository.save(refreshTokenEntity);

        mockMvc.perform(post("/api/v1/auth/refreshToken")
                        .cookie(new Cookie("accessToken", jwtToken), new Cookie("refreshToken", refreshTokenEntity.getToken())))
                .andExpect(status().isInternalServerError());
    }
}
