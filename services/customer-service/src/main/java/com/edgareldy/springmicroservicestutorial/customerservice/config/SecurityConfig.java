package com.edgareldy.springmicroservicestutorial.customerservice.config;

import com.edgareldy.springmicroservicestutorial.customerservice.security.CustomAccessDeniedHandler;
import com.edgareldy.springmicroservicestutorial.customerservice.security.CustomAuthenticationEntryPoint;
import com.edgareldy.springmicroservicestutorial.customerservice.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Wires the stateless, JWT-based {@link SecurityFilterChain} for
 * {@code customer-service}: no sessions, no CSRF (a stateless API using
 * bearer tokens is not vulnerable to the cookie-based CSRF attack this
 * protects against), {@link JwtAuthFilter} inserted ahead of the standard
 * username/password filter, and the custom 401/403 handlers.
 * <p>
 * Unlike {@code catalog-service}, the README's endpoint table for this
 * branch (`/api/v1/customers/**`) lists no {@code (ADMIN)} qualifier on any
 * method, GET/POST/PUT alike. Read literally, that could mean every one of
 * these endpoints is meant to stay public. This class deliberately does
 * not do that: {@code customers} rows hold PII (name, phone, e-mail,
 * address), and leaving PII readable/writable by anyone with network access
 * to the service would be inconsistent with the JWT-secured posture every
 * other business service in this project takes (see {@code
 * auth-service}/{@code catalog-service}'s own {@code SecurityConfig}).
 * So every {@code /api/v1/customers/**} route here requires a valid,
 * {@code auth-service}-issued JWT ({@code authenticated()}), but stops short
 * of a specific role/permission check ({@code hasRole(...)}) since the
 * README defines none for this service and inventing one here would be an
 * undocumented authorization rule a client couldn't have anticipated from
 * the spec. This also means no {@code MethodSecurityConfig}/
 * {@code @EnableMethodSecurity} is added in this branch: nothing here uses
 * {@code @PreAuthorize}, so enabling it would add a config class with no
 * actual effect. Unlike {@code auth-service}, there is no
 * {@code AuthenticationManager} bean here: this service never authenticates
 * a username/password pair itself, it only trusts a JWT already issued by
 * {@code auth-service}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_DOCS = {
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_DOCS).permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        // No (ADMIN)-style qualifier for this service's endpoints in the
                        // README, but customer data is PII: every /api/v1/customers/**
                        // route still requires a valid JWT, see class Javadoc.
                        .requestMatchers("/api/v1/customers/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
