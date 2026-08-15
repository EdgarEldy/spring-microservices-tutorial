package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import com.edgareldy.springmicroservicestutorial.authservice.mapper.PermissionMapperImpl;
import com.edgareldy.springmicroservicestutorial.authservice.repository.PermissionRepository;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure Mockito unit tests for {@link PermissionServiceImpl}: {@link PermissionRepository}
 * is mocked, no Spring context and no database, complementing the
 * Testcontainers-backed {@code PermissionRepositoryTest}. The MapStruct-generated
 * {@link PermissionMapperImpl} is instantiated for real (not mocked) and wired
 * manually rather than via {@code @InjectMocks}: mocking a trivial generated
 * mapper would add nothing and would require stubbing every field of every
 * {@code toResponse} call.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    private PermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionServiceImpl(permissionRepository, new PermissionMapperImpl());
    }

    @Test
    void create_newResourceActionPair_persistsAndReturnsPermission() {
        PermissionRequest request = new PermissionRequest("PRODUCT", "WRITE");
        when(permissionRepository.existsByResourceIgnoreCaseAndActionIgnoreCase("PRODUCT", "WRITE"))
                .thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Permission created = permissionService.create(request);

        assertThat(created.getResource()).isEqualTo("PRODUCT");
        assertThat(created.getAction()).isEqualTo("WRITE");
        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    void create_duplicateResourceActionPair_throwsBusinessRuleExceptionAndNeverSaves() {
        PermissionRequest request = new PermissionRequest("PRODUCT", "WRITE");
        when(permissionRepository.existsByResourceIgnoreCaseAndActionIgnoreCase("PRODUCT", "WRITE"))
                .thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> permissionService.create(request));

        verify(permissionRepository, never()).save(any());
    }

    @Test
    void findAll_delegatesToRepository() {
        Permission permission = Permission.builder().id(1L).resource("PRODUCT").action("WRITE").build();
        when(permissionRepository.findAll()).thenReturn(List.of(permission));

        assertThat(permissionService.findAll()).containsExactly(permission);
    }

    @Test
    void delete_found_deletesPermission() {
        Permission permission = Permission.builder().id(1L).resource("PRODUCT").action("WRITE").build();
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));

        permissionService.delete(1L);

        verify(permissionRepository).delete(permission);
    }

    @Test
    void delete_notFound_throwsResourceNotFoundExceptionAndNeverDeletes() {
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> permissionService.delete(99L));

        verify(permissionRepository, never()).delete(any());
    }

    @Test
    void toResponse_mapsFields() {
        Permission permission = Permission.builder().id(1L).resource("PRODUCT").action("WRITE").build();

        PermissionResponse response = permissionService.toResponse(permission);

        assertThat(response).isEqualTo(new PermissionResponse(1L, "PRODUCT", "WRITE"));
    }
}
