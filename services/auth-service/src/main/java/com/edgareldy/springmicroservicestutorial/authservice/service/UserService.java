package com.edgareldy.springmicroservicestutorial.authservice.service;

import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.RegisterRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UserResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;

/**
 * Contract for creating and querying {@link User} accounts. Returns the
 * entity itself (not {@link UserResponse}) from the mutating/lookup methods
 * so callers such as {@code AuthServiceImpl} can build a response or a Kafka
 * event payload from it as needed, without this contract having to guess
 * what each caller needs.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface UserService {

    /**
     * Creates a new, disabled, non-locked {@code User} with an encoded
     * password. Throws {@code BusinessRuleException} if the email is
     * already in use.
     */
    User createUser(RegisterRequest request);

    /**
     * Looks up a user by email (case-insensitive). Throws
     * {@code ResourceNotFoundException} if none matches.
     */
    User findByEmail(String email);

    /**
     * Looks up a user by id. Throws {@code ResourceNotFoundException} if
     * none matches.
     */
    User findById(Long userId);

    /** Marks the given user's account as enabled, once its activation token is validated. */
    void enableAccount(Long userId);

    /** Encodes and persists a new password for the given user. */
    void updatePassword(Long userId, String newRawPassword);

    /** Maps a {@link User} entity to its public, credential-free {@link UserResponse} view. */
    UserResponse toResponse(User user);

    /** Returns whether a user with this email (case-insensitive) already exists. */
    boolean existsByEmail(String email);
}
