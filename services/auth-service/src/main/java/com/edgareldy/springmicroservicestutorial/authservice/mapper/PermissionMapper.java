package com.edgareldy.springmicroservicestutorial.authservice.mapper;

import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper translating between {@link Permission} entities and their
 * request/response DTOs. Both directions are pure field-for-field mappings
 * ({@code id}/{@code resource}/{@code action} for {@code toResponse};
 * {@code resource}/{@code action} for {@code toEntity}, {@code id} left null
 * for a new, unsaved entity), so no explicit {@code @Mapping} is needed
 * beyond ignoring {@code id} on creation.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse toResponse(Permission permission);

    @Mapping(target = "id", ignore = true)
    Permission toEntity(PermissionRequest request);
}
