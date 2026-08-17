package com.edgareldy.springmicroservicestutorial.catalogservice.security;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Writes a 403 response in the shared {@link ApiResponse} envelope whenever
 * an authenticated but unauthorized request (e.g. a non-{@code ADMIN}
 * calling {@code POST /api/v1/catalog/products}) is rejected by the
 * security filter chain. Duplicated from {@code auth-service}'s class of
 * the same name for the same reason documented on
 * {@link CustomAuthenticationEntryPoint}: this is service-specific security
 * wiring, never {@code common-lib} material.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("Access denied")));
    }
}
