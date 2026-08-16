package com.edgareldy.springmicroservicestutorial.apigateway;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * End-to-end coverage of {@code api-gateway}'s routing and JWT enforcement, wired into a real
 * {@code SecurityFilterChain}-equivalent (Spring Cloud Gateway's own global filter chain, with
 * {@link com.edgareldy.springmicroservicestutorial.apigateway.filter.JwtValidationGatewayFilter}
 * genuinely registered as a {@code GlobalFilter} bean) rather than the isolated Mockito-only
 * coverage in {@code JwtValidationGatewayFilterTest}. Backs the README's "routing to a mocked
 * downstream", "JWT rejection on a protected route without a token", and "pass-through on
 * public routes" test requirements; the fourth ("rate limiter returning 429") is covered
 * separately by {@code RateLimiterIntegrationTest}, which needs a real Redis.
 * <p>
 * The real {@code lb://}-based routes live only in {@code config-repo/api-gateway.yml},
 * unreachable here (this profile disables the config-server import, and there is no real
 * Eureka registry in a test anyway, see {@link TestRoutesConfig}'s own Javadoc). WireMock
 * stands in for whatever downstream service a route would normally proxy to.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Import(ApiGatewayRoutingAndSecurityTest.TestRoutesConfig.class)
class ApiGatewayRoutingAndSecurityTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private WebTestClient webTestClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/orders/1")).willReturn(okJson("{\"orderId\":1}")));
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/auth/register")).willReturn(okJson("{\"registered\":true}")));
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("wiremock.base-url", () -> "http://localhost:" + wireMockServer.port());
    }

    @Test
    void routingToMockedDownstream_validToken_proxiesRequestAndResponse() {
        webTestClient
                .get()
                .uri("/api/v1/orders/1")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.orderId")
                .isEqualTo(1);
    }

    @Test
    void jwtRejection_protectedRouteNoToken_rejectedBeforeReachingDownstream() {
        webTestClient
                .get()
                .uri("/api/v1/orders/1")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void passThroughOnPublicRoute_noToken_stillReachesDownstream() {
        webTestClient
                .post()
                .uri("/api/v1/auth/register")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.registered")
                .isEqualTo(true);

        verify(postRequestedFor(urlEqualTo("/api/v1/auth/register")));
    }

    private String validToken() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("ada@example.com")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(5))))
                .signWith(key)
                .compact();
    }

    /**
     * Defines two ad hoc routes straight at WireMock's dynamic port, standing in for the
     * {@code lb://auth-service}/{@code lb://order-service} routes {@code RouteConfig} (this
     * module's own production route definitions) points at real business services: a test has
     * no live Eureka registry to resolve {@code lb://} against. {@code RouteConfig} is plain
     * component-scanned Java, always active regardless of profile, so this bean is deliberately
     * named {@code "routes"} too, the same bean name {@code RouteConfig.routes} uses: with
     * {@code spring.main.allow-bean-definition-overriding=true} (set in
     * {@code application-test.yml}), this REPLACES the production bean for this test context
     * rather than adding a second, colliding {@link RouteLocator}. {@link
     * com.edgareldy.springmicroservicestutorial.apigateway.filter.JwtValidationGatewayFilter}
     * is still a real, separately-registered {@code GlobalFilter} bean and applies to these
     * routes exactly as it would to the production ones.
     */
    @TestConfiguration
    static class TestRoutesConfig {

        @Bean("routes")
        RouteLocator testRoutes(RouteLocatorBuilder builder, @Value("${wiremock.base-url}") String baseUrl) {
            return builder.routes()
                    .route("test-protected-orders", r -> r.path("/api/v1/orders/**").uri(baseUrl))
                    .route("test-public-auth-register", r -> r.path("/api/v1/auth/register").uri(baseUrl))
                    .build();
        }
    }
}
