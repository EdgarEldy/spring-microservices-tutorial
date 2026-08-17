package com.edgareldy.springmicroservicestutorial.authservice.service;

import com.edgareldy.springmicroservicestutorial.authservice.entity.ActivationToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;

/**
 * Contract for issuing and consuming {@link ActivationToken}s, the
 * single-use link that turns a freshly registered, disabled {@link User}
 * into an enabled one.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface ActivationTokenService {

    /**
     * Generates and persists a new activation token for the given user,
     * expiring 24 hours from now.
     */
    ActivationToken generate(User user);

    /**
     * Validates a raw token value: throws
     * {@code InvalidTokenException} if it does not exist, was already
     * validated, or has expired; otherwise marks it validated and returns
     * the owning {@link User}.
     */
    User validate(String rawToken);
}
