package com.edgareldy.springmicroservicestutorial.authservice.security;

import com.edgareldy.springmicroservicestutorial.authservice.repository.BlacklistedTokenRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts the JWT from the {@code Authorization: Bearer <token>} header,
 * rejects it if its {@code jti} is blacklisted (logged out), and otherwise
 * populates {@link SecurityContextHolder} so the rest of the filter chain
 * and controllers see an authenticated {@link UserDetails}.
 * <p>
 * Any parsing/validation failure (missing, expired, tampered, blacklisted,
 * unknown user) simply leaves the request unauthenticated rather than
 * rejecting it directly here: the request then either reaches a public
 * endpoint successfully, or is denied later by {@code SecurityConfig}'s
 * authorization rules via {@code CustomAuthenticationEntryPoint}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final UserDetailsServiceImpl userDetailsService;

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
            String jti = jwtService.extractJti(token);
            if (blacklistedTokenRepository.existsByJti(jti)) {
                // Logged-out token: leave the request unauthenticated rather
                // than throwing, same treatment as any other invalid token.
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String email = jwtService.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | UsernameNotFoundException ex) {
            // Swallowed on purpose (see class Javadoc): logged at DEBUG so the
            // failure is still visible without rejecting the request here.
            log.debug("Rejected invalid JWT: {}", ex.getMessage());
        }
    }
}
