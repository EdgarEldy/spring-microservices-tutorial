package com.edgareldy.springmicroservicestutorial.authservice.mapper;

import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper translating {@link Role} entities into {@link RoleResponse}
 * records. {@code id} and {@code roleName} map directly; the nested
 * {@code Set<Permission> permissions} is converted to {@code List<PermissionResponse>}
 * by delegating each element to {@link PermissionMapper}, injected here via
 * {@code uses}. MapStruct supports the {@code Set -> List} collection shape
 * change natively, so no extra configuration is required for that part.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Mapper(componentModel = "spring", uses = PermissionMapper.class)
public interface RoleMapper {

    RoleResponse toResponse(Role role);
}
