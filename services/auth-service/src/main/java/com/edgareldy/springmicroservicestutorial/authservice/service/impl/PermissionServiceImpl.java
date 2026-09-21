package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import com.edgareldy.springmicroservicestutorial.authservice.mapper.PermissionMapper;
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
 * {@link PermissionRepository} and delegating entity-DTO mapping in both
 * directions to {@link PermissionMapper}. Every method maps its result to a
 * {@link PermissionResponse} before returning, since nothing outside this
 * service ever needs the raw {@link Permission} entity.
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
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional
    public PermissionResponse create(PermissionRequest request) {
        if (permissionRepository.existsByResourceIgnoreCaseAndActionIgnoreCase(request.resource(), request.action())) {
            throw new BusinessRuleException(
                    "Permission already exists: " + request.resource() + ":" + request.action());
        }
        Permission permission = permissionMapper.toEntity(request);
        Permission saved = permissionRepository.save(permission);
        return permissionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> findAll() {
        return permissionRepository.findAll().stream().map(permissionMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("No permission found with id " + permissionId));
        permissionRepository.delete(permission);
    }
}
