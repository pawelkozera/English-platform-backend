package com.learning.english.integration;

import com.learning.english.models.User;
import com.learning.english.repository.RefreshTokenRepository;
import com.learning.english.repository.UserRepository;
import com.learning.english.service.JwtService;
import com.learning.english.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthenticationControllerTest {

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
    private static final String TEST_PASSWORD = "a";
    private static final String TEST_FIRST_NAME = "Test";
    private static final String TEST_LAST_NAME = "User";

    private String jwtToken;
    private String refreshToken;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .email(TEST_EMAIL)
                .password("$2a$10$kvkhBZvBHKA/VRQlUpD42.a2WjFTkhU5Rvg/Xx4bgU3CMvTuF7ybi")
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
    void testSignupSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content("{\"firstName\":\"" + TEST_FIRST_NAME + "\",\"lastName\":\"" + TEST_LAST_NAME + "\",\"email\":\"newuser@example.com\",\"password\":\"" + TEST_PASSWORD + "\"}")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void testSignupFailureUserAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content("{\"firstName\":\"" + TEST_FIRST_NAME + "\",\"lastName\":\"" + TEST_LAST_NAME + "\",\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + TEST_PASSWORD + "\"}")
                )
                .andExpect(status().isConflict()) // Conflict because user already exists
                .andExpect(content().string("{\"status\":409,\"message\":\"Please complete the registration by following the instructions sent to your email address.\",\"path\":null}"));
    }

    @Test
    void testSigninSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType("application/json")
                        .content("{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + TEST_PASSWORD + "\"}")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("User logged in successfully"));
    }

    @Test
    void testSigninFailureInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType("application/json")
                        .content("{\"email\":\"invalidemail@example.com\",\"password\":\"wrongpassword\"}")
                )
                .andExpect(status().isInternalServerError()) // Unauthorized due to wrong credentials
                .andExpect(content().string("{\"status\":500,\"message\":\"An unexpected error occurred.\",\"path\":null}"));
    }

    @Test
    void testLogoutSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("accessToken", jwtToken), new Cookie("refreshToken", refreshToken))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));
    }
}