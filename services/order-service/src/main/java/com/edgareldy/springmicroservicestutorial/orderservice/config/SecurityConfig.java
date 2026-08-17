package com.edgareldy.springmicroservicestutorial.orderservice.config;

import com.edgareldy.springmicroservicestutorial.orderservice.security.CustomAccessDeniedHandler;
import com.edgareldy.springmicroservicestutorial.orderservice.security.CustomAuthenticationEntryPoint;
import com.edgareldy.springmicroservicestutorial.orderservice.security.JwtAuthFilter;
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
 * Wires the stateless, JWT-based {@link SecurityFilterChain} for {@code order-service}: no
 * sessions, no CSRF (a stateless API using bearer tokens is not vulnerable to the cookie-based
 * CSRF attack this protects against), {@link JwtAuthFilter} inserted ahead of the standard
 * username/password filter, and the custom 401/403 handlers.
 * <p>
 * Every {@code /api/v1/orders/**} route requires {@code authenticated()}, no role/permission
 * qualifier (the README's endpoint table for this branch lists none, same reasoning
 * {@code customer-service}'s {@code SecurityConfig} already documents for its own PII-bearing
 * data). Here there is a second, load-bearing reason beyond data sensitivity: {@link
 * FeignConfig}'s interceptor forwards the caller's own {@code Authorization} header on to
 * {@code CustomerClient}, which itself requires a valid JWT; if this service's own endpoints
 * were left public, there would be no caller JWT to forward in the first place, and every
 * {@code CustomerClient} call would fail with 401 regardless of who called
 * {@code order-service}. Unlike {@code auth-service}, there is no {@code AuthenticationManager}
 * bean here: this service never authenticates a username/password pair itself, it only trusts
 * a JWT already issued by {@code auth-service}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
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
                        .requestMatchers("/api/v1/orders/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
