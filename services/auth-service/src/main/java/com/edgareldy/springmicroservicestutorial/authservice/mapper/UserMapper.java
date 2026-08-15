package com.edgareldy.springmicroservicestutorial.authservice.mapper;

import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UserResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper translating {@link User} entities into {@link UserResponse}
 * records. {@code id}, {@code firstName}, {@code lastName}, {@code email},
 * {@code enabled} and {@code accountLocked} map field-for-field, but
 * {@code User.roles} ({@code Set<Role>}) to {@code UserResponse.roles}
 * ({@code List<String>} of role names) is not a mapping MapStruct can infer
 * from field names alone, so it is expressed explicitly via a default
 * method ({@code mapRoleNames}) referenced through {@code @Mapping(expression = ...)},
 * the simplest and most idiomatic way MapStruct supports a one-off custom
 * conversion without introducing a separate qualifier-annotated mapper class.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(mapRoleNames(user.getRoles()))")
    UserResponse toResponse(User user);

    /**
     * Flattens assigned roles down to their names. Iteration order follows
     * {@code roles}' own (a {@code HashSet} by default on {@link User}), so
     * callers should not rely on a stable order, same as the manual mapping
     * this replaces.
     */
    default List<String> mapRoleNames(Set<Role> roles) {
        return roles.stream().map(Role::getRoleName).toList();
    }
}
