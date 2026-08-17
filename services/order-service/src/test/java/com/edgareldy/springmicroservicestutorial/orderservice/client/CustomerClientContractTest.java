package com.edgareldy.springmicroservicestutorial.orderservice.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.client.dto.CustomerResponse;
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
 * Consumer-driven contract test for {@link CustomerClient}, mirroring {@link
 * ProductClientContractTest}'s own rationale exactly (see that class' Javadoc for the full
 * explanation of how {@code @AutoConfigureStubRunner(stubsMode = LOCAL)} resolves the stub and
 * why no {@code @DynamicPropertySource} override of {@code customer-service.url} is needed
 * here either): resolves {@code com.edgareldy:customer-service:+:stubs} from the local {@code
 * .m2} repository and lets Spring Cloud LoadBalancer's stub-runner integration register it
 * under the {@code customer-service} name {@link CustomerClient} already resolves via Eureka in
 * a real environment, replacing {@link CustomerClientTest}'s hand-written 200-response WireMock
 * stub with the real one {@code customer-service} generates from its own contract.
 * <p>
 * Same trade-off as {@link ProductClientContractTest}: only the *shape* of the successful
 * response is covered here. {@link CustomerClientTest}'s 404/401 tests stay hand-written
 * against a real WireMock server, since a contract never encodes a failure response.
 * <p>
 * Created by Edgar Muhamyangabo on 8/17/26
 * Author : Edgar Muhamyangabo
 * Date : 8/17/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = CustomerClientContractTest.TestConfig.class)
@TestPropertySource(properties = "eureka.client.enabled=false")
@AutoConfigureStubRunner(
        ids = "com.edgareldy:customer-service:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL)
class CustomerClientContractTest {

    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @EnableFeignClients(clients = CustomerClient.class)
    static class TestConfig {
    }

    @Autowired
    private CustomerClient customerClient;

    @Test
    void getCustomer_success_deserializesApiResponseEnvelopeAndCustomerResponse() {
        ApiResponse<CustomerResponse> response = customerClient.getCustomer(1L);

        assertThat(response.success()).isTrue();
        assertThat(response.data().id()).isEqualTo(1L);
        assertThat(response.data().firstName()).isEqualTo("Ada");
        assertThat(response.data().email()).isEqualTo("ada@example.com");
    }
}
