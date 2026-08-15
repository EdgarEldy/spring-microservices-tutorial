package com.edgareldy.springmicroservicestutorial.authservice.controller;

import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import com.edgareldy.springmicroservicestutorial.authservice.service.PermissionService;
import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes {@code /api/v1/permissions/**}: standalone resource/action permission
 * administration, entirely restricted to ADMIN at the class level.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Permissions", description = "Resource/action permission administration, ADMIN only")
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "List all permissions")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> findAll() {
        List<PermissionResponse> permissions =
                permissionService.findAll().stream().map(permissionService::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(permissions, "Permissions retrieved"));
    }

    @Operation(summary = "Create a permission")
    @PostMapping
    public ResponseEntity<ApiResponse<PermissionResponse>> create(@Valid @RequestBody PermissionRequest request) {
        Permission permission = permissionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(permissionService.toResponse(permission), "Permission created"));
    }

    @Operation(summary = "Delete a permission")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Permission deleted"));
    }
}
