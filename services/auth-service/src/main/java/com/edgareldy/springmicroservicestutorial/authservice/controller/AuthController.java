package com.edgareldy.springmicroservicestutorial.authservice.controller;

import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.AuthResponse;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.ForgotPasswordRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.LoginRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.RegisterRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.ResetPasswordRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UserResponse;
import com.edgareldy.springmicroservicestutorial.authservice.service.AuthService;
import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the seven {@code /api/v1/auth/**} flows from the README's endpoint table
 * (register, activate, login, logout, me, forgot-password, reset-password), delegating
 * every one of them to {@link AuthService} and wrapping the result in the shared
 * {@link ApiResponse} envelope. Which endpoints are public versus authenticated is decided
 * once, in {@code SecurityConfig.PUBLIC_ENDPOINTS}; this class does not repeat that
 * decision, it only implements the behavior for each path.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, activation, login/logout, and password reset")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @Operation(summary = "Register a new account",
            description = "Creates a disabled user and its activation token, publishes UserRegisteredEvent")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Account created, check your email to activate it"));
    }

    @Operation(summary = "Activate an account", description = "Validates the activation token and enables the account")
    @GetMapping("/activate-account")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@RequestParam String token) {
        authService.activateAccount(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Account activated"));
    }

    @Operation(summary = "Login", description = "Authenticates the given credentials and returns a signed JWT")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @Operation(summary = "Logout", description = "Blacklists the current JWT so it can no longer be used")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        String rawToken = authorizationHeader.startsWith(BEARER_PREFIX)
                ? authorizationHeader.substring(BEARER_PREFIX.length())
                : authorizationHeader;
        authService.logout(rawToken);
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    @Operation(summary = "Current user", description = "Returns the profile of the currently authenticated user")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        UserResponse response = authService.me();
        return ResponseEntity.ok(ApiResponse.success(response, "Current user profile"));
    }

    @Operation(summary = "Forgot password",
            description = "Generates a password-reset token, publishes PasswordResetRequestedEvent")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset instructions sent"));
    }

    @Operation(summary = "Reset password", description = "Consumes the reset token and updates the password")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successful"));
    }
}
