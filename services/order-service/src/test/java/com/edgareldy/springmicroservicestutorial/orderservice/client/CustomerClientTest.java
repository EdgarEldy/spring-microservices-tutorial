package com.edgareldy.springmicroservicestutorial.orderservice.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * WireMock-backed test for {@link CustomerClient}'s failure paths, mirroring {@link
 * ProductClientTest}'s own rationale and minimal-context setup exactly (see that class'
 * Javadoc, including why the success-path test moved out to {@link
 * CustomerClientContractTest}). {@code customer-service.url} is overridden instead of
 * {@code catalog-service.url}. Unlike {@code customer-service}'s real {@code SecurityConfig}
 * (which requires {@code authenticated()} on every {@code /api/v1/customers/**} route), the
 * WireMock stub here does not enforce that: it only proves {@link CustomerClient}'s own
 * HTTP/JSON wiring is correct on failure, the {@code Authorization}-forwarding behaviour itself
 * is {@link com.edgareldy.springmicroservicestutorial.orderservice.config.FeignConfig}'s
 * concern, not this client's.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = CustomerClientTest.TestConfig.class)
@TestPropertySource(properties = "eureka.client.enabled=false")
class CustomerClientTest {

    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @EnableFeignClients(clients = CustomerClient.class)
    static class TestConfig {
    }

    private static WireMockServer wireMockServer;

    @Autowired
    private CustomerClient customerClient;

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
    static void registerCustomerServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("customer-service.url", () -> wireMockServer.baseUrl());
    }

    @Test
    void getCustomer_notFound_throwsFeignExceptionNotFound() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/customers/99"))
                .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"success":false,"message":"No customer found with id 99","data":null,"timestamp":"2026-08-16T00:00:00Z"}
                                """)));

        assertThatThrownBy(() -> customerClient.getCustomer(99L)).isInstanceOf(FeignException.NotFound.class);
    }

    @Test
    void getCustomer_unauthorized_throwsFeignException() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/customers/1"))
                .willReturn(aResponse().withStatus(401)));

        assertThatThrownBy(() -> customerClient.getCustomer(1L)).isInstanceOf(FeignException.class);
    }
}
