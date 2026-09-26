package com.edgareldy.springmicroservicestutorial.customerservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmicroservicestutorial.customerservice.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the full Spring context starts successfully, wiring every bean this service
 * declares against a real Testcontainers PostgreSQL database ({@link TestcontainersConfig}).
 * Unlike every other test in this module (pure Mockito unit tests or narrow {@code
 * @WebMvcTest}/{@code @DataJpaTest} slices, all of which mock {@code JwtService} away or never
 * construct a full context at all), this is the one test that actually exercises the whole
 * bean graph together with a real {@code jwt.secret} resolved from configuration. This is
 * exactly the kind of test that would have caught {@code jwt.secret} being missing from
 * {@code config-repo/customer-service-dev.yml}/{@code customer-service-prod.yml} (only the
 * local {@code application-test.yml} copy existed, so no narrower test ever noticed the real
 * dev/prod config was incomplete). Runs on a random port ({@code WebEnvironment.RANDOM_PORT})
 * so it never collides with a real instance running locally on the fixed {@code server.port}
 * from {@code application.yml}. {@code @ActiveProfiles("test")} activates
 * {@code application-test.yml} (which disables the config-server import and supplies
 * {@code jwt.secret} locally).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class CustomerServiceApplicationTests {

    @Test
    void contextLoads(ApplicationContext context) {
        assertThat(context).isNotNull();
        assertThat(context.containsBean("jwtService")).isTrue();
    }
}
