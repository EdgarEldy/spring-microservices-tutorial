package com.edgareldy.springmicroservicestutorial.apigateway.filter;

import com.edgareldy.springmicroservicestutorial.apigateway.security.JwtService;
import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rejects any request to a protected route that does not carry a JWT verifiable with {@link
 * JwtService}, before it is proxied to a downstream service. The three routes the README
 * exempts ({@code POST /api/v1/auth/register}, {@code POST /api/v1/auth/login}, {@code GET
 * /api/v1/auth/activate-account}) are matched by exact path: {@code auth-service}'s
 * {@code AuthController} has other routes under {@code /api/v1/auth/**} (logout, me,
 * forgot-password, reset-password) that do require a token, so a prefix match would
 * incorrectly wave those through too.
 * <p>
 * A rejected request never reaches the downstream service at all: this filter writes the 401
 * response itself, in the same {@link ApiResponse} envelope every business service already
 * uses for its own {@code CustomAuthenticationEntryPoint}, and completes the exchange without
 * calling {@code chain.filter(exchange)}. An accepted request is forwarded unchanged,
 * including its original {@code Authorization} header: this filter only verifies the token
 * once at the edge, it does not re-issue or rewrite it, since every downstream service also
 * parses the same header itself (see the README's "validated at the gateway and again at
 * each service" rule).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Component
public class JwtValidationGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationGatewayFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private static final Set<String> PUBLIC_PATHS =
            Set.of("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/activate-account");

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtValidationGatewayFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (PUBLIC_PATHS.contains(request.getURI().getPath())) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return reject(exchange, "Authentication required");
        }

        try {
            jwtService.validate(authHeader.substring(BEARER_PREFIX.length()));
        } catch (JwtException ex) {
            log.debug("Rejected invalid JWT: {}", ex.getMessage());
            return reject(exchange, "Invalid or expired token");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Runs before routing-affecting filters (load balancing, etc.): no reason to
        // resolve a downstream instance for a request this filter is about to reject.
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(ApiResponse.error(message));
        } catch (Exception ex) {
            body = ("{\"success\":false,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
