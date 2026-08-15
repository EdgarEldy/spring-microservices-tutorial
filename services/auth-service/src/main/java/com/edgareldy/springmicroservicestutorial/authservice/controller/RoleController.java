package com.edgareldy.springmicroservicestutorial.authservice.controller;

import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import com.edgareldy.springmicroservicestutorial.authservice.service.RoleService;
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
 * Exposes {@code /api/v1/roles/**}: RBAC administration, entirely restricted to ADMIN at the
 * class level, covering role CRUD plus granting/revoking a permission on a role.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Roles", description = "RBAC role administration, ADMIN only")
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "List all roles")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> findAll() {
        List<RoleResponse> roles = roleService.findAll().stream().map(roleService::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(roles, "Roles retrieved"));
    }

    @Operation(summary = "Create a role")
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest request) {
        Role role = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(roleService.toResponse(role), "Role created"));
    }

    @Operation(summary = "Delete a role")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted"));
    }

    @Operation(summary = "Grant a permission to a role")
    @PostMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<RoleResponse>> addPermission(
            @PathVariable Long roleId, @PathVariable Long permissionId) {
        Role role = roleService.addPermission(roleId, permissionId);
        return ResponseEntity.ok(ApiResponse.success(roleService.toResponse(role), "Permission granted"));
    }

    @Operation(summary = "Revoke a permission from a role")
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<RoleResponse>> removePermission(
            @PathVariable Long roleId, @PathVariable Long permissionId) {
        Role role = roleService.removePermission(roleId, permissionId);
        return ResponseEntity.ok(ApiResponse.success(roleService.toResponse(role), "Permission revoked"));
    }
}
