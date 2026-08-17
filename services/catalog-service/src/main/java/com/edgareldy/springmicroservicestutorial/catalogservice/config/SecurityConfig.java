package com.edgareldy.springmicroservicestutorial.catalogservice.config;

import com.edgareldy.springmicroservicestutorial.catalogservice.security.CustomAccessDeniedHandler;
import com.edgareldy.springmicroservicestutorial.catalogservice.security.CustomAuthenticationEntryPoint;
import com.edgareldy.springmicroservicestutorial.catalogservice.security.JwtAuthFilter;
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
 * {@code catalog-service}: no sessions, no CSRF (a stateless API using
 * bearer tokens is not vulnerable to the cookie-based CSRF attack this
 * protects against), every {@code GET /api/v1/catalog/**} left public per
 * the README's endpoint table, every {@code POST /api/v1/catalog/**}
 * requiring the {@code ADMIN} role, {@link JwtAuthFilter} inserted ahead of
 * the standard username/password filter, and the custom 401/403 handlers.
 * Unlike {@code auth-service}, there is no {@code AuthenticationManager}
 * bean here: this service never authenticates a username/password pair
 * itself, it only trusts a JWT already issued by {@code auth-service}.
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
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/catalog/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
