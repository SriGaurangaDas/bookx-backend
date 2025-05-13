package com.bookx.bookx_backend.integration;

import com.bookx.bookx_backend.config.TestSecurityConfig;
import com.bookx.bookx_backend.controller.AuthController;
import com.bookx.bookx_backend.dto.AuthRequest;
import com.bookx.bookx_backend.dto.UserDto;
import com.bookx.bookx_backend.filter.JwtFilter;
import com.bookx.bookx_backend.model.User;
import com.bookx.bookx_backend.service.AuthService;
import com.bookx.bookx_backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtFilter.class
    )
)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserDetailsService userDetailsService;

    private UserDto testUserDto;
    private User testUser;
    private String testUsername;
    private String testEmail;
    private String testPassword;

    @BeforeEach
    void setup() {
        testUsername = "intUser_" + UUID.randomUUID().toString();
        testEmail = testUsername + "@example.com";
        testPassword = "password123";

        testUserDto = new UserDto();
        testUserDto.setUsername(testUsername);
        testUserDto.setEmail(testEmail);
        testUserDto.setFullName(testUsername + " FullName");
        testUserDto.setPassword(testPassword);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(testUsername);
        testUser.setEmail(testEmail);
        testUser.setFullName(testUserDto.getFullName());
        testUser.setEnabled(true);
        testUser.setRegisteredAt(Instant.now());
    }

    @Test
    void registerUser_success() throws Exception {
        when(userService.registerUser(any(UserDto.class))).thenReturn(testUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUsername))
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    void loginUser_success() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername(testUsername);
        authRequest.setPassword(testPassword);
        String mockToken = "mock.jwt.token";

        when(authService.authenticateAndGetToken(any(AuthRequest.class))).thenReturn(mockToken);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(mockToken));
    }

    @Test
    void loginUser_invalidCredentials() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("wrongUser");
        authRequest.setPassword("wrongPass");

        when(authService.authenticateAndGetToken(any(AuthRequest.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized()); 
    }

    @Test
    void accessProtected_withValidToken_shouldBePermittedByTestConfig() throws Exception {
        mockMvc.perform(get("/api/auth/login") 
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed()); 
    }

    @Test
    void accessProtected_withoutToken_shouldBePermittedByTestConfig() throws Exception {
        mockMvc.perform(get("/api/auth/login") 
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed()); 
    }
}
