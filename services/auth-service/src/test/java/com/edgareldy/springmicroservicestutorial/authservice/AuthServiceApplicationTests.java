package com.edgareldy.springmicroservicestutorial.authservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmicroservicestutorial.authservice.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the full Spring context starts successfully, wiring every bean this service
 * declares against a real Testcontainers PostgreSQL database ({@link TestcontainersConfig}).
 * Unlike every other test in this module (pure Mockito unit tests or narrow {@code
 * @WebMvcTest}/{@code @DataJpaTest} slices), this is the one test that actually exercises the
 * whole bean graph together, which is exactly what caught this service missing the
 * {@code spring-boot-kafka} dependency: {@code AuthEventProducer}'s {@code KafkaTemplate}
 * dependency was never satisfiable without it, but no narrower test ever constructed
 * {@code AuthEventProducer} as a real Spring bean to notice. Runs on a random port
 * ({@code WebEnvironment.RANDOM_PORT}) so it never collides with a real instance running
 * locally on the fixed {@code server.port} from {@code application.yml}. {@code
 * @ActiveProfiles("test")} activates {@code application-test.yml} (which disables the
 * config-server import and supplies {@code jwt.secret} locally, see that file's own
 * comment): no other test in this module needed it, since every {@code @WebMvcTest} mocks
 * {@code JwtService} away and every {@code @DataJpaTest}/pure-Mockito test never constructs
 * it at all, making this the first test to actually require a real one.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class AuthServiceApplicationTests {

    @Test
    void contextLoads(ApplicationContext context) {
        assertThat(context).isNotNull();
        assertThat(context.containsBean("kafkaTemplate")).isTrue();
    }
}
