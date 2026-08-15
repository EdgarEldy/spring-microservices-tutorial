package com.edgareldy.springmicroservicestutorial.authservice.mapper;

import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper translating {@link Permission} entities into the
 * outward-facing {@link PermissionResponse} record. {@code id}, {@code resource}
 * and {@code action} map field-for-field, so no explicit {@code @Mapping} is
 * needed: MapStruct generates the implementation purely from the two types.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse toResponse(Permission permission);
}
