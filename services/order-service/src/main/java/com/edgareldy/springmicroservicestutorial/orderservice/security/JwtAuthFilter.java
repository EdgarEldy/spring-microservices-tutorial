package com.edgareldy.springmicroservicestutorial.orderservice.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts the JWT from the {@code Authorization: Bearer <token>} header and populates
 * {@link SecurityContextHolder} directly from its claims - unlike {@code auth-service}'s
 * equivalent filter, there is no {@code UserDetailsService} to reload a principal from, since
 * {@code order-service} owns no user data at all: the token is the only source of truth this
 * service ever consults. Duplicated from {@code catalog-service}/{@code customer-service}'s
 * class of the same name, same rationale as {@link JwtService}.
 * <p>
 * {@code auth-service}'s {@code JwtService.generateToken} puts role names in the
 * {@code roles} claim upper-cased but <em>without</em> a {@code ROLE_} prefix (that prefix is
 * only added by {@code User.getAuthorities()} when building the {@code Authentication} object
 * over there); this filter adds the same {@code ROLE_} prefix itself here, plus a
 * {@code PERMISSION_} prefix for the {@code permissions} claim, kept for consistency even
 * though this branch's {@code SecurityConfig} only ever checks {@code authenticated()}, never
 * a specific role or permission.
 * <p>
 * Any parsing/validation failure (missing, expired, tampered token) simply leaves the request
 * unauthenticated rather than rejecting it directly here: the request is then denied later by
 * {@code SecurityConfig}'s authorization rules via {@code CustomAuthenticationEntryPoint},
 * same pattern as every other business service.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            authenticate(token);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                return;
            }
            String email = jwtService.extractUsername(token);
            Set<GrantedAuthority> authorities = new HashSet<>();
            jwtService.extractRoles(token).forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            jwtService.extractPermissions(token)
                    .forEach(permission -> authorities.add(new SimpleGrantedAuthority("PERMISSION_" + permission)));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException ex) {
            // Swallowed on purpose (see class Javadoc): logged at DEBUG so the
            // failure is still visible without rejecting the request here.
            log.debug("Rejected invalid JWT: {}", ex.getMessage());
        }
    }
}
