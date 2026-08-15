package com.edgareldy.springmicroservicestutorial.authservice.mapper;

import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.RegisterRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UserResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * MapStruct mapper translating between {@link User} entities and their request/response
 * DTOs. Declared as an abstract class rather than an interface (unlike the other mappers
 * in this package) because {@code toEntity(RegisterRequest)} needs a real collaborator,
 * {@link PasswordEncoder}, injected as a protected field the way MapStruct expects extra
 * dependencies to be wired into a generated implementation.
 * <p>
 * {@code toResponse}: {@code id}, {@code firstName}, {@code lastName}, {@code email},
 * {@code enabled} and {@code accountLocked} map field-for-field, but {@code User.roles}
 * ({@code Set<Role>}) to {@code UserResponse.roles} ({@code List<String>} of role names)
 * is not a mapping MapStruct can infer from field names alone, so it is expressed
 * explicitly via a protected method ({@code mapRoleNames}) referenced through
 * {@code @Mapping(expression = ...)}.
 * <p>
 * {@code toEntity(RegisterRequest)}: {@code firstName}/{@code lastName}/{@code email} map
 * directly; {@code enabled}/{@code accountLocked} are forced to {@code false} (a brand new
 * account is always disabled and unlocked, {@link RegisterRequest} carries neither field
 * at all); {@code password} is run through {@link #encodePassword(String)} rather than
 * copied raw, since {@code RegisterRequest.password()} is the plaintext value the caller
 * typed in; {@code roles} is left unmapped (this service assigns no default role on
 * registration, unlike some reference implementations that auto-grant a "USER" role here).
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Mapping(target = "roles", expression = "java(mapRoleNames(user.getRoles()))")
    public abstract UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", constant = "false")
    @Mapping(target = "accountLocked", constant = "false")
    @Mapping(target = "password", qualifiedByName = "encodePassword")
    @Mapping(target = "roles", ignore = true)
    public abstract User toEntity(RegisterRequest request);

    /**
     * Flattens assigned roles down to their names. Iteration order follows
     * {@code roles}' own (a {@code HashSet} by default on {@link User}), so
     * callers should not rely on a stable order, same as the manual mapping
     * this replaces.
     */
    protected List<String> mapRoleNames(Set<Role> roles) {
        return roles.stream().map(Role::getRoleName).toList();
    }

    /**
     * Hashes a raw password with the shared {@link PasswordEncoder} bean before it is
     * ever persisted; referenced from {@link #toEntity(RegisterRequest)} via
     * {@code qualifiedByName} rather than inlined, so the encoding step has a single,
     * named, testable entry point.
     */
    @Named("encodePassword")
    protected String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
