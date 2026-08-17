package com.edgareldy.springmicroservicestutorial.catalogservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables {@code @PreAuthorize} on controller methods, independently of the
 * HTTP filter chain {@link SecurityConfig} registers, for defense in depth.
 * Only {@code hasRole(...)} expressions are used in this service (no
 * {@code hasPermission(...)}), so unlike {@code auth-service}'s equivalent
 * config, no custom {@code PermissionEvaluator} needs wiring here.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
