package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import com.edgareldy.springmicroservicestutorial.authservice.mapper.PermissionMapperImpl;
import com.edgareldy.springmicroservicestutorial.authservice.mapper.RoleMapperImpl;
import com.edgareldy.springmicroservicestutorial.authservice.repository.PermissionRepository;
import com.edgareldy.springmicroservicestutorial.authservice.repository.RoleRepository;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure Mockito unit tests for {@link RoleServiceImpl}: {@link RoleRepository}
 * and {@link PermissionRepository} are mocked, no Spring context and no
 * database, complementing the Testcontainers-backed {@code RoleRepositoryTest}.
 * The MapStruct-generated {@link RoleMapperImpl} (itself backed by a real
 * {@link PermissionMapperImpl}) is instantiated for real and wired manually
 * rather than via {@code @InjectMocks}: mocking a trivial generated mapper
 * would add nothing and would require stubbing every field of every
 * {@code toResponse} call. {@code componentModel = "spring"} makes MapStruct
 * generate field injection ({@code @Autowired private PermissionMapper}), not
 * a constructor parameter, so the dependency is wired via
 * {@link ReflectionTestUtils#setField} after construction rather than passed
 * to a constructor.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        RoleMapperImpl roleMapper = new RoleMapperImpl();
        ReflectionTestUtils.setField(roleMapper, "permissionMapper", new PermissionMapperImpl());
        roleService = new RoleServiceImpl(roleRepository, permissionRepository, roleMapper);
    }

    @Test
    void _01_ShouldPersistAndReturnRole_WhenRoleNameIsNew() {
        RoleRequest request = new RoleRequest("ADMIN");
        when(roleRepository.existsByRoleNameIgnoreCase("ADMIN")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse created = roleService.create(request);

        assertThat(created.roleName()).isEqualTo("ADMIN");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void _02_ShouldThrowBusinessRuleExceptionAndNeverSave_WhenRoleNameIsDuplicate() {
        RoleRequest request = new RoleRequest("ADMIN");
        when(roleRepository.existsByRoleNameIgnoreCase("ADMIN")).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> roleService.create(request));

        verify(roleRepository, never()).save(any());
    }

    @Test
    void _03_ShouldDelegateToRepository_WhenAllRolesAreRequested() {
        Role role = Role.builder().id(1L).roleName("ADMIN").permissions(Set.of()).build();
        when(roleRepository.findAll()).thenReturn(List.of(role));

        assertThat(roleService.findAll()).containsExactly(new RoleResponse(1L, "ADMIN", List.of()));
    }

    @Test
    void _04_ShouldReturnRole_WhenRoleIsFound() {
        Role role = Role.builder().id(1L).roleName("ADMIN").build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        assertThat(roleService.findById(1L)).isEqualTo(role);
    }

    @Test
    void _05_ShouldThrowResourceNotFoundException_WhenRoleIsNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> roleService.findById(99L));
    }

    @Test
    void _06_ShouldAddToRoleAndSave_WhenPermissionIsAdded() {
        Role role = Role.builder().id(1L).roleName("ADMIN").permissions(new HashSet<>()).build();
        Permission permission = Permission.builder().id(2L).resource("PRODUCT").action("WRITE").build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission));
        when(roleRepository.save(role)).thenReturn(role);

        RoleResponse updated = roleService.addPermission(1L, 2L);

        assertThat(updated.permissions()).hasSize(1);
        assertThat(updated.permissions().get(0).resource()).isEqualTo("PRODUCT");
        verify(roleRepository).save(role);
    }

    @Test
    void _07_ShouldThrowResourceNotFoundException_WhenPermissionToAddIsNotFound() {
        Role role = Role.builder().id(1L).roleName("ADMIN").permissions(new HashSet<>()).build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> roleService.addPermission(1L, 99L));

        verify(roleRepository, never()).save(any());
    }

    @Test
    void _08_ShouldRemoveFromRoleAndSave_WhenPermissionIsRemoved() {
        Permission permission = Permission.builder().id(2L).resource("PRODUCT").action("WRITE").build();
        Role role = Role.builder().id(1L).roleName("ADMIN").permissions(new HashSet<>(Set.of(permission))).build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission));
        when(roleRepository.save(role)).thenReturn(role);

        RoleResponse updated = roleService.removePermission(1L, 2L);

        assertThat(updated.permissions()).isEmpty();
        verify(roleRepository).save(role);
    }

    @Test
    void _09_ShouldDeleteRole_WhenRoleIsFound() {
        Role role = Role.builder().id(1L).roleName("ADMIN").build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        roleService.delete(1L);

        verify(roleRepository).delete(role);
    }

    @Test
    void _10_ShouldThrowResourceNotFoundExceptionAndNeverDelete_WhenRoleIsNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> roleService.delete(99L));

        verify(roleRepository, never()).delete(any());
    }
}
