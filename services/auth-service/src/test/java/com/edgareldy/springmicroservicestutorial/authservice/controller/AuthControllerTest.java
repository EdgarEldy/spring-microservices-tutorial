package com.edgareldy.springmicroservicestutorial.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.AuthResponse;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.ForgotPasswordRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.LoginRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.RegisterRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.ResetPasswordRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UserResponse;
import com.edgareldy.springmicroservicestutorial.authservice.exception.InvalidTokenException;
import com.edgareldy.springmicroservicestutorial.authservice.repository.BlacklistedTokenRepository;
import com.edgareldy.springmicroservicestutorial.authservice.security.JwtService;
import com.edgareldy.springmicroservicestutorial.authservice.security.UserDetailsServiceImpl;
import com.edgareldy.springmicroservicestutorial.authservice.service.AuthService;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc slice tests for {@link AuthController}, with {@link AuthService} mocked so no
 * real registration/login/token logic runs. Servlet filters are disabled
 * ({@code addFilters = false}): {@code /me} and {@code /logout} rely on {@code @WithMockUser}
 * rather than a real JWT going through {@code JwtAuthFilter}. {@code JwtAuthFilter} still needs
 * to be instantiated though, since {@code @WebMvcTest} auto-includes any {@code Filter} bean
 * regardless of {@code addFilters}; its own collaborators ({@link JwtService},
 * {@link BlacklistedTokenRepository}, {@link UserDetailsServiceImpl}) are mocked purely so the
 * application context loads.
 * <p>
 * {@code AuthExceptionHandler.handleValidation} maps a malformed {@code @Valid} payload
 * ({@code MethodArgumentNotValidException}) to a 400, so {@code registerRejectsInvalidPayload}/
 * {@code loginRejectsInvalidPayload} below assert that, not the 500 a missing handler would have
 * produced.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void registerReturns201WithCreatedUser() throws Exception {
        RegisterRequest request = new RegisterRequest("Jane", "Doe", "jane@example.com", "password1");
        UserResponse response = new UserResponse(1L, "Jane", "Doe", "jane@example.com", false, false, List.of());
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("jane@example.com"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void registerReturns400WhenBusinessRuleViolated() throws Exception {
        RegisterRequest request = new RegisterRequest("Jane", "Doe", "jane@example.com", "password1");
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessRuleException("Email already in use"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    /**
     * A malformed {@code @Valid} payload is mapped to 400 by
     * {@code AuthExceptionHandler.handleValidation}, not the catch-all {@code Exception} handler.
     */
    @Test
    void registerRejectsInvalidPayload() throws Exception {
        RegisterRequest request = new RegisterRequest("", "", "not-an-email", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(authService, never()).register(any());
    }

    @Test
    void activateAccountReturns200AndDelegatesToService() throws Exception {
        mockMvc.perform(get("/api/v1/auth/activate-account").param("token", "activation-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).activateAccount("activation-token");
    }

    @Test
    void activateAccountReturns400WhenTokenInvalid() throws Exception {
        org.mockito.Mockito.doThrow(new InvalidTokenException("Activation token not found"))
                .when(authService).activateAccount("bad-token");

        mockMvc.perform(get("/api/v1/auth/activate-account").param("token", "bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Activation token not found"));
    }

    @Test
    void loginReturns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("jane@example.com", "password1");
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse("jwt-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-token"));
    }

    @Test
    void loginReturns401OnBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("jane@example.com", "wrong-password");
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    /**
     * Same validation coverage as {@link #registerRejectsInvalidPayload()}, for {@code LoginRequest}.
     */
    @Test
    void loginRejectsInvalidPayload() throws Exception {
        LoginRequest request = new LoginRequest("not-an-email", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(authService, never()).login(any());
    }

    @Test
    @WithMockUser(username = "jane@example.com")
    void logoutReturns200AndExtractsBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authService).logout("jwt-token");
    }

    @Test
    @WithMockUser(username = "jane@example.com")
    void meReturns200WithCurrentUserProfile() throws Exception {
        UserResponse response = new UserResponse(1L, "Jane", "Doe", "jane@example.com", true, false, List.of());
        when(authService.me()).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("jane@example.com"));
    }

    @Test
    void forgotPasswordReturns200AndDelegatesToService() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("jane@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).forgotPassword("jane@example.com");
    }

    @Test
    void resetPasswordReturns200AndDelegatesToService() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "newPassword1");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).resetPassword("reset-token", "newPassword1");
    }

    @Test
    void resetPasswordReturns400WhenTokenInvalid() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("bad-token", "newPassword1");
        org.mockito.Mockito.doThrow(new InvalidTokenException("Reset token expired"))
                .when(authService).resetPassword("bad-token", "newPassword1");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Reset token expired"));
    }
}
