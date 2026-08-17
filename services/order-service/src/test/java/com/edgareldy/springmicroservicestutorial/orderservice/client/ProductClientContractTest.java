package com.edgareldy.springmicroservicestutorial.orderservice.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.client.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.TestPropertySource;

/**
 * Consumer-driven contract test for {@link ProductClient} (see feature/contract-testing):
 * replaces {@link ProductClientTest}'s hand-written 200-response WireMock stub with the real
 * stub jar {@code catalog-service} generates from its own {@code
 * spring-cloud-starter-contract-verifier} contract ({@code
 * catalog-service/src/test/resources/contracts/product/findProductById.yml}). If that contract
 * (or its implementation, {@code catalog-service}'s own {@code ProductController}) changes
 * shape without the contract being updated to match, {@code catalog-service}'s own build fails
 * first; if the contract itself changes shape, this test fails here, in {@code order-service}'s
 * own build, without either service needing to be deployed anywhere.
 * <p>
 * {@code @AutoConfigureStubRunner(stubsMode = LOCAL)} resolves {@code
 * com.edgareldy:catalog-service:+:stubs} straight from the local {@code .m2} repository (the
 * producer's {@code mvn install} already published it there): no remote Nexus/Artifactory is
 * configured for this tutorial. No explicit port and no {@code @DynamicPropertySource}
 * override of {@code catalog-service.url} are needed, unlike {@link ProductClientTest}'s
 * hand-rolled WireMock server: stub runner's own Spring Cloud LoadBalancer integration ({@code
 * StubRunnerServiceInstanceListSupplier}, brought in automatically since {@code
 * spring-cloud-starter-loadbalancer} is already a transitive dependency of {@code
 * spring-cloud-starter-netflix-eureka-client}) registers the stub's random port under the
 * producer's own {@code spring.application.name} ({@code catalog-service}, matching {@link
 * ProductClient}'s {@code @FeignClient(name = "catalog-service")}), the same name {@link
 * ProductClient} would resolve via Eureka in a real environment. Verified empirically that this
 * keeps working with {@code eureka.client.enabled=false} (this test's own minimal context,
 * mirroring {@link ProductClientTest}'s, never starts a real Eureka client): stub runner's
 * {@code ServiceInstanceListSupplier} bean does not depend on Eureka at all, only on Spring
 * Cloud LoadBalancer being on the classpath.
 * <p>
 * Trade-off (see README's feature/contract-testing task list): this only replaces the
 * *shape*-verification {@link ProductClientTest}'s success test used to do. It does not, and
 * cannot, replace {@link ProductClientTest}'s own 404/500 tests or {@link
 * ProductClientCircuitBreakerTest}'s simulated-outage test: a contract only ever encodes a
 * producer's one documented successful response, never the failure responses a consumer must
 * also defend against, so those stay hand-written against a real WireMock server.
 * <p>
 * Created by Edgar Muhamyangabo on 8/17/26
 * Author : Edgar Muhamyangabo
 * Date : 8/17/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = ProductClientContractTest.TestConfig.class)
@TestPropertySource(properties = "eureka.client.enabled=false")
@AutoConfigureStubRunner(
        ids = "com.edgareldy:catalog-service:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL)
class ProductClientContractTest {

    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @EnableFeignClients(clients = ProductClient.class)
    static class TestConfig {
    }

    @Autowired
    private ProductClient productClient;

    @Test
    void getProduct_success_deserializesApiResponseEnvelopeAndProductResponse() {
        ApiResponse<ProductResponse> response = productClient.getProduct(1L);

        assertThat(response.success()).isTrue();
        assertThat(response.data().id()).isEqualTo(1L);
        assertThat(response.data().productName()).isEqualTo("Clean Code");
        assertThat(response.data().unitPrice()).isEqualTo(39.90);
    }
}
