package com.edgareldy.springmicroservicestutorial.customerservice.security;

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
 * an authenticated but unauthorized request is rejected by the security
 * filter chain. In this branch that only happens if a future
 * {@code @PreAuthorize} is added without a matching authority (today, every
 * {@code /api/v1/customers/**} rule in {@code SecurityConfig} is a plain
 * {@code authenticated()}, no role/permission check), but the handler is
 * still wired for defense in depth and for consistency with {@code
 * auth-service}/{@code catalog-service}'s identical class, never {@code
 * common-lib} material (see {@link CustomAuthenticationEntryPoint}).
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
