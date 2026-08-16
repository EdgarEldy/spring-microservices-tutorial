package com.edgareldy.springmicroservicestutorial.apigateway;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies the Redis-backed {@code RequestRateLimiter} filter genuinely returns 429 once a
 * client exceeds the configured threshold, against a real Redis (Testcontainers
 * {@code redis:7-alpine}), not an in-memory fake standing in for the Lua-script-based token
 * bucket {@link RedisRateLimiter} actually runs. Backs the README's "rate limiter returning
 * 429 past the configured threshold" test requirement; the other three test requirements for
 * this branch are covered by {@code ApiGatewayRoutingAndSecurityTest}.
 * <p>
 * The test route's {@code replenishRate}/{@code burstCapacity} are both set to {@code 1} (see
 * {@link TestRateLimiterRouteConfig}'s own Javadoc for why that has to be done by hand rather
 * than through a property) so the bucket is exhausted almost immediately rather than requiring
 * this test to wait out a realistic production replenishment window (higher and time-based,
 * see {@code gateway.rate-limiter.login.*} in {@code api-gateway.yml}). {@link
 * #requestRateLimiter_pastBurstCapacity_returns429} still fires several rapid requests rather
 * than assuming the 2nd one alone is the rejected one: the Lua script's elapsed-time
 * calculation means a slow test JVM (a loaded reactor build running several Testcontainers
 * modules at once) can let a token regenerate between two sequential calls, which would
 * otherwise make an assertion pinned to "exactly the 2nd request" an occasional false
 * negative.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Import(RateLimiterIntegrationTest.TestRateLimiterRouteConfig.class)
class RateLimiterIntegrationTest {

    // Started eagerly in this initializer (not via the org.testcontainers:junit-jupiter
    // @Testcontainers/@Container extension, an extra dependency this module does not
    // otherwise need) so it is already running by the time @DynamicPropertySource below
    // reads its host/mapped port, same manual-lifecycle approach as wireMockServer.
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static WireMockServer wireMockServer;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startContainers() {
        redis.start();
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/auth/login")).willReturn(okJson("{\"loggedIn\":true}")));
    }

    @AfterAll
    static void stopContainers() {
        wireMockServer.stop();
        redis.stop();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("wiremock.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    void requestRateLimiter_pastBurstCapacity_returns429() {
        // Fires several rapid requests rather than asserting the 2nd one alone trips the
        // limit: with replenishRate=1/burstCapacity=1, the very first request already
        // exhausts the bucket, so the 2nd should normally be the one rejected, but the Lua
        // script computes elapsed time in fractional seconds - under a loaded CI/reactor
        // build, enough wall-clock time can pass between two sequential WebTestClient calls
        // for a full token to have already regenerated, letting the 2nd slip through as a
        // false negative. Asserting "at least one of several rapid requests is rejected" is
        // still a genuine assertion that the real Redis-backed limiter rejects something,
        // without being coupled to exactly which call number that happens to be.
        boolean sawTooManyRequests = false;
        for (int i = 0; i < 10 && !sawTooManyRequests; i++) {
            HttpStatusCode status = webTestClient
                    .post()
                    .uri("/api/v1/auth/login")
                    .exchange()
                    .returnResult(Void.class)
                    .getStatus();
            sawTooManyRequests = status.equals(HttpStatus.TOO_MANY_REQUESTS);
        }
        assertThat(sawTooManyRequests).isTrue();
    }

    /**
     * Same rationale as {@code ApiGatewayRoutingAndSecurityTest.TestRoutesConfig}: no live
     * Eureka registry to resolve {@code lb://auth-service} against in a test, so this route
     * points straight at WireMock instead, with the exact same {@code RequestRateLimiter}
     * filter (backed by the real, auto-configured {@link RedisRateLimiter} bean and this
     * module's real {@code clientIpKeyResolver} bean) that {@code RouteConfig} (this module's
     * own production route definitions) attaches to {@code POST /api/v1/auth/login}. Named
     * {@code "routes"}, same bean name {@code RouteConfig.routes} uses, so (with {@code
     * spring.main.allow-bean-definition-overriding=true}, set in {@code application-test.yml})
     * it REPLACES that production bean here rather than adding a second, colliding {@link
     * RouteLocator}.
     * <p>
     * {@link RedisRateLimiter}'s per-route {@code replenishRate}/{@code burstCapacity} (its own
     * {@code RedisRateLimiter.Config}, distinct from the generic {@code
     * RequestRateLimiterGatewayFilterFactory.Config} the fluent {@code requestRateLimiter(...)}
     * DSL call above configures) is normally populated by a {@code FilterArgsEvent} that {@code
     * RouteDefinitionRouteLocator} publishes while binding a YAML route's {@code args:} map;
     * {@link RouteLocatorBuilder}'s Java DSL (used both here and by {@code RouteConfig}) never
     * publishes that event, so without the explicit {@code redisRateLimiter.getConfig().put(...)}
     * below, {@code RedisRateLimiter.isAllowed} would fail every request with "No Configuration
     * found for route ... or defaultFilters" rather than actually rate-limiting anything (see
     * {@code RouteConfig}'s own Javadoc, which documents the identical issue for the real route).
     */
    @TestConfiguration
    static class TestRateLimiterRouteConfig {

        private static final String ROUTE_ID = "test-login-rate-limited";

        @Bean("routes")
        RouteLocator testRateLimiterRoute(
                RouteLocatorBuilder builder,
                RedisRateLimiter redisRateLimiter,
                KeyResolver clientIpKeyResolver,
                @Value("${wiremock.base-url}") String baseUrl) {
            redisRateLimiter
                    .getConfig()
                    .put(
                            ROUTE_ID,
                            new RedisRateLimiter.Config()
                                    .setReplenishRate(1)
                                    .setBurstCapacity(1)
                                    .setRequestedTokens(1));

            return builder.routes()
                    .route(
                            ROUTE_ID,
                            r -> r.path("/api/v1/auth/login")
                                    .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter)
                                            .setKeyResolver(clientIpKeyResolver)))
                                    .uri(baseUrl))
                    .build();
        }
    }
}
