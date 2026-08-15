package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import com.edgareldy.springmicroservicestutorial.authservice.repository.PermissionRepository;
import com.edgareldy.springmicroservicestutorial.authservice.service.PermissionService;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link PermissionService} implementation, backed by
 * {@link PermissionRepository}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public Permission create(PermissionRequest request) {
        if (permissionRepository.existsByResourceIgnoreCaseAndActionIgnoreCase(request.resource(), request.action())) {
            throw new BusinessRuleException(
                    "Permission already exists: " + request.resource() + ":" + request.action());
        }
        Permission permission = Permission.builder()
                .resource(request.resource())
                .action(request.action())
                .build();
        return permissionRepository.save(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    @Override
    @Transactional
    public void delete(Long permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("No permission found with id " + permissionId));
        permissionRepository.delete(permission);
    }

    @Override
    public PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getResource(), permission.getAction());
    }
}
