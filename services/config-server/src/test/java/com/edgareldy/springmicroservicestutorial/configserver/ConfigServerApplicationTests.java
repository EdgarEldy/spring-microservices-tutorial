package com.edgareldy.springmicroservicestutorial.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Spring Cloud Config Server's context starts with
 * {@code @EnableConfigServer} active, and that the native/classpath-backed config repo
 * under {@code config-repo/} is actually served over HTTP: this is the mechanism this
 * branch exists to deliver, so asserting the context loads alone would not be enough.
 * Runs on a random port ({@code WebEnvironment.RANDOM_PORT}) to avoid colliding with the
 * fixed {@code server.port: 8888} from {@code application.yml}. Uses {@link RestTestClient}
 * (Spring Framework 7's replacement for the removed {@code TestRestTemplate}) bound
 * directly to the running server via its base URL.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerApplicationTests {

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
        assertThat(port).isPositive();
    }

    @Test
    void servesCatalogServiceConfigFromNativeConfigRepo() {
        RestTestClient client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        client.get().uri("/catalog-service/default")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("\"name\":\"catalog-service\"");
                    assertThat(body).contains("server.port");
                    assertThat(body).contains("8082");
                });
    }
}
