package com.edgareldy.springmicroservicestutorial.orderservice.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.client.dto.ProductResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * WireMock-backed test for {@link ProductClient}, verifying its actual HTTP/JSON wiring
 * against a real (stubbed) server rather than a Mockito mock of the interface: that
 * {@code ApiResponse<ProductResponse>} deserializes correctly from a genuine 200 response, and
 * that a real 404 response genuinely surfaces as {@code FeignException.NotFound} (the
 * exception type {@code OrderServiceImpl.resolveProduct} pattern-matches on), not just
 * assumed. Backs the README's "WireMock stubs for ProductClient/CustomerClient (success and
 * failure)" test requirement.
 * <p>
 * Uses a minimal, hand-picked {@code @SpringBootTest(classes = ...)} context rather than the
 * full {@code OrderServiceApplication} one: {@code @EnableAutoConfiguration} alone (no
 * {@code @ComponentScan}) plus {@code @EnableFeignClients(clients = ProductClient.class)}
 * brings up only the Feign infrastructure this client needs, with
 * {@code DataSourceAutoConfiguration}/{@code HibernateJpaAutoConfiguration}/{@code
 * DataJpaRepositoriesAutoConfiguration}/{@code FlywayAutoConfiguration} excluded since this
 * test never touches persistence at all. {@code catalog-service.url} is overridden to point
 * {@link ProductClient} straight at WireMock, bypassing Eureka/LoadBalancer resolution
 * entirely (see that client's own Javadoc); {@code eureka.client.enabled=false} additionally
 * stops the context from wasting time on background Eureka registration attempts it does not
 * need for this test.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = ProductClientTest.TestConfig.class)
@TestPropertySource(properties = "eureka.client.enabled=false")
class ProductClientTest {

    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @EnableFeignClients(clients = ProductClient.class)
    static class TestConfig {
    }

    private static WireMockServer wireMockServer;

    @Autowired
    private ProductClient productClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @AfterEach
    void resetStubs() {
        wireMockServer.resetAll();
    }

    @DynamicPropertySource
    static void registerCatalogServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("catalog-service.url", () -> wireMockServer.baseUrl());
    }

    @Test
    void getProduct_success_deserializesApiResponseEnvelopeAndProductResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/catalog/products/1"))
                .willReturn(okJson("""
                        {"success":true,"message":"Product retrieved","data":{"id":1,"productName":"Clean Code","unitPrice":39.90,"categoryId":1},"timestamp":"2026-08-16T00:00:00Z"}
                        """)));

        ApiResponse<ProductResponse> response = productClient.getProduct(1L);

        assertThat(response.success()).isTrue();
        assertThat(response.data().id()).isEqualTo(1L);
        assertThat(response.data().productName()).isEqualTo("Clean Code");
        assertThat(response.data().unitPrice()).isEqualTo(39.90);
    }

    @Test
    void getProduct_notFound_throwsFeignExceptionNotFound() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/catalog/products/99"))
                .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"success":false,"message":"No product found with id 99","data":null,"timestamp":"2026-08-16T00:00:00Z"}
                                """)));

        assertThatThrownBy(() -> productClient.getProduct(99L)).isInstanceOf(FeignException.NotFound.class);
    }

    @Test
    void getProduct_serverError_throwsFeignException() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/catalog/products/1"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> productClient.getProduct(1L)).isInstanceOf(FeignException.class);
    }
}
