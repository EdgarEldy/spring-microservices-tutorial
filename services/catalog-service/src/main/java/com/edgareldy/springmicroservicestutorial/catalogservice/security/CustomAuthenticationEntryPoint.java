package com.edgareldy.springmicroservicestutorial.catalogservice.security;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Writes a 401 response in the shared {@link ApiResponse} envelope whenever
 * an unauthenticated request hits a protected endpoint. Duplicated from
 * {@code auth-service}'s class of the same name rather than shared via
 * {@code common-lib}: this class depends on Spring Security types tied to
 * this service's own filter chain wiring, and {@code common-lib} never
 * holds code specific to one service's security configuration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("Authentication required")));
    }
}
