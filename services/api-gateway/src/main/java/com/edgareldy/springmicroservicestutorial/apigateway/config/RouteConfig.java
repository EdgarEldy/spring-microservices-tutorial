package com.edgareldy.springmicroservicestutorial.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines every route this gateway proxies to, resolved through Eureka/Spring Cloud
 * LoadBalancer via the {@code lb://<service-name>} URI scheme (see the README's "Load
 * balancing"), never a fixed host. {@code notification-service} has no route here: it exposes
 * no REST endpoints at all (see its own module Javadoc), nothing to proxy to.
 * <p>
 * {@code auth-service-login} is declared ahead of the general {@code auth-service} route on
 * purpose: {@link RouteLocatorBuilder.Builder#route} matches routes in declaration order, and
 * this one needs to win for {@code POST /api/v1/auth/login} specifically so the {@code
 * RequestRateLimiter} filter applies to it, before the catch-all {@code auth-service} route
 * (with no rate limiter) would otherwise match the same path first.
 * <p>
 * {@link RedisRateLimiter}'s own per-route {@code replenishRate}/{@code burstCapacity}
 * (distinct from the generic {@code RequestRateLimiterGatewayFilterFactory.Config} the fluent
 * {@code requestRateLimiter(...)} call below configures) is normally populated by a {@code
 * FilterArgsEvent} published while binding a YAML route's {@code args:} map; a route built
 * through this Java DSL never publishes that event, so it is registered by hand via {@code
 * redisRateLimiter.getConfig().put(...)} below instead. Without it, every login request would
 * fail with "No Configuration found for route auth-service-login or defaultFilters" rather
 * than actually being rate-limited (the exact failure this class' own tests, see {@code
 * RateLimiterIntegrationTest}, exist to catch).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Configuration
public class RouteConfig {

    private static final String LOGIN_ROUTE_ID = "auth-service-login";

    @Bean
    public RouteLocator routes(
            RouteLocatorBuilder builder,
            RedisRateLimiter redisRateLimiter,
            KeyResolver clientIpKeyResolver,
            @Value("${gateway.rate-limiter.login.replenish-rate:5}") int loginReplenishRate,
            @Value("${gateway.rate-limiter.login.burst-capacity:10}") int loginBurstCapacity) {
        redisRateLimiter
                .getConfig()
                .put(
                        LOGIN_ROUTE_ID,
                        new RedisRateLimiter.Config()
                                .setReplenishRate(loginReplenishRate)
                                .setBurstCapacity(loginBurstCapacity)
                                .setRequestedTokens(1));

        return builder.routes()
                .route(
                        LOGIN_ROUTE_ID,
                        r -> r.path("/api/v1/auth/login")
                                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter)
                                        .setKeyResolver(clientIpKeyResolver)))
                                .uri("lb://auth-service"))
                .route(
                        "auth-service",
                        r -> r.path("/api/v1/auth/**", "/api/v1/users/**", "/api/v1/roles/**", "/api/v1/permissions/**")
                                .uri("lb://auth-service"))
                .route("catalog-service", r -> r.path("/api/v1/catalog/**").uri("lb://catalog-service"))
                .route("customer-service", r -> r.path("/api/v1/customers/**").uri("lb://customer-service"))
                .route("order-service", r -> r.path("/api/v1/orders/**").uri("lb://order-service"))
                .build();
    }
}
