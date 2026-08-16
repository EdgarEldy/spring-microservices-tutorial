package com.edgareldy.springmicroservicestutorial.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.apigateway.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Pure Mockito unit tests for {@link JwtValidationGatewayFilter}: {@link JwtService} and {@link
 * GatewayFilterChain} are mocked, {@link MockServerWebExchange} stands in for a real request,
 * no Spring context and no real downstream route is involved. End-to-end routing behaviour
 * (this filter wired into a real filter chain, in front of a real proxied route) is instead
 * covered by {@code ApiGatewayRoutingAndSecurityTest}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class JwtValidationGatewayFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private GatewayFilterChain chain;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JwtValidationGatewayFilter filter;

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/activate-account"})
    void filter_eachOfTheThreeExemptedPublicPaths_bypassesValidationAndAlwaysForwards(String path) {
        filter = new JwtValidationGatewayFilter(jwtService, objectMapper);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
        verify(jwtService, never()).validate(any());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/v1/orders",
                "/api/v1/auth/logout",
                "/api/v1/auth/me",
                "/api/v1/auth/forgot-password",
                "/api/v1/auth/reset-password"
            })
    void filter_protectedPathNoAuthorizationHeader_rejectsWithoutCallingChain(String path) {
        filter = new JwtValidationGatewayFilter(jwtService, objectMapper);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_protectedPathInvalidToken_rejectsWithoutCallingChain() {
        filter = new JwtValidationGatewayFilter(jwtService, objectMapper);
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders").header("Authorization", "Bearer bad-token"));
        doThrow(new JwtException("expired")).when(jwtService).validate("bad-token");

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_protectedPathValidToken_forwardsToChain() {
        filter = new JwtValidationGatewayFilter(jwtService, objectMapper);
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders").header("Authorization", "Bearer good-token"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(jwtService).validate("good-token");
        verify(chain).filter(exchange);
    }
}
