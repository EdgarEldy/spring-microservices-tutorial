package com.edgareldy.springmicroservicestutorial.authservice.mapper;

import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper translating between {@link Role} entities and their request/response
 * DTOs. {@code toResponse}: {@code id} and {@code roleName} map directly, the nested
 * {@code Set<Permission> permissions} is converted to {@code List<PermissionResponse>} by
 * delegating each element to {@link PermissionMapper}, injected here via {@code uses}
 * (MapStruct supports the {@code Set -> List} collection shape change natively).
 * {@code toEntity}: only {@code roleName} comes from {@link RoleRequest}, {@code id} is
 * ignored (left null for a new, unsaved entity) and {@code permissions} is left at its
 * field-initializer default (an empty set, whether MapStruct constructs the target via
 * {@code Role}'s Lombok {@code @Builder} or its no-args constructor + setters, both run
 * the field initializer); permission assignment happens through a separate endpoint, not
 * at role creation.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Mapper(componentModel = "spring", uses = PermissionMapper.class)
public interface RoleMapper {

    RoleResponse toResponse(Role role);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    Role toEntity(RoleRequest request);
}
