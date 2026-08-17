package com.edgareldy.springmicroservicestutorial.authservice.controller;

import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UpdateProfileRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UserResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import com.edgareldy.springmicroservicestutorial.authservice.service.UserService;
import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes {@code /api/v1/users/**}, deliberately limited to what
 * {@link UserService} exposes today. Not part of the README's own
 * {@code feature/auth-service} endpoint table (which only lists
 * {@code /api/v1/auth/**}), so its shape is a project decision documented
 * here rather than specified: two endpoints, {@code GET /{id}} and
 * {@code PUT /{id}}, both restricted to the account owner or an ADMIN. No
 * {@code GET /} list endpoint is added, since {@link UserService} has no
 * paginated {@code findAll} to back it, and adding one to the service purely
 * to satisfy a controller nobody asked for would be scope creep beyond this
 * branch's task list. {@code updateProfile(Long, UpdateProfileRequest)} was
 * added to {@link UserService}/{@code UserServiceImpl} in this same change,
 * a minor, non-domain-shifting addition that gives the already-existing
 * {@link UpdateProfileRequest} DTO a caller.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile lookup and update, restricted to the account owner or an ADMIN")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get a user by id", description = "Restricted to the account owner or an ADMIN")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(userService.toResponse(user), "User found"));
    }

    @Operation(summary = "Update a user's profile", description = "Restricted to the account owner or an ADMIN")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success(userService.toResponse(user), "Profile updated"));
    }
}
