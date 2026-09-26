package com.edgareldy.springmicroservicestutorial.discoveryserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Eureka service registry's Spring context starts successfully with
 * {@code @EnableEurekaServer} active; there is no business logic to exercise here, so a
 * context-load smoke test is the appropriate depth of testing for this infrastructure
 * service. Runs on a random port ({@code WebEnvironment.RANDOM_PORT}) rather than the
 * fixed {@code server.port: 8761} from {@code application.yml}, so the test never
 * collides with a real Eureka instance already running locally on 8761.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServerApplicationTests {

    @Test
    void contextLoads(ApplicationContext context) {
        assertThat(context).isNotNull();
        assertThat(context.containsBean("eurekaServerBootstrap")).isTrue();
    }
}
