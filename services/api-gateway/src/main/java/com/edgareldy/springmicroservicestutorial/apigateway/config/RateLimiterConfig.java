package com.edgareldy.springmicroservicestutorial.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Supplies the {@link KeyResolver} the {@code RequestRateLimiter} route filter (configured in
 * {@code api-gateway.yml}, on {@code POST /api/v1/auth/login} only) uses to bucket requests
 * per client IP, protecting {@code auth-service} against brute-force login attempts (see the
 * README). The actual token bucket (replenish rate/burst capacity) is Spring Cloud Gateway's
 * own auto-configured {@code RedisRateLimiter} bean, backed by
 * {@code spring-boot-starter-data-redis-reactive} on the classpath: no bean for it is defined
 * here, only the per-request key it buckets by.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver clientIpKeyResolver() {
        return exchange -> {
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            String ip = (remoteAddress != null && remoteAddress.getAddress() != null)
                    ? remoteAddress.getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}
