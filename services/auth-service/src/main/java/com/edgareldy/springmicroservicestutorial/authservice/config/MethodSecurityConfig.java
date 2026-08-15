package com.edgareldy.springmicroservicestutorial.authservice.config;

import com.edgareldy.springmicroservicestutorial.authservice.security.CustomPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables {@code @PreAuthorize}/{@code @PostAuthorize} on service and
 * controller methods, independently of the HTTP filter chain
 * {@link SecurityConfig} registers. The
 * {@link MethodSecurityExpressionHandler} bean wires
 * {@link CustomPermissionEvaluator} in so
 * {@code hasPermission('RESOURCE', 'ACTION')} expressions are evaluated by
 * it instead of Spring Security's default (which always denies without a
 * configured {@code PermissionEvaluator}).
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CustomPermissionEvaluator customPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(customPermissionEvaluator);
        return expressionHandler;
    }
}
