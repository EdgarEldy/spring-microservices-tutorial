package com.edgareldy.springmicroservicestutorial.authservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Shared Testcontainers configuration for {@code @DataJpaTest} repository
 * tests: starts a single real {@code postgres:16} container per test class
 * and wires it into the Spring context via {@code @ServiceConnection},
 * matching the image used by {@code docker-compose.yml} so behaviour
 * observed here (e.g. functional indexes, {@code TIMESTAMPTZ} mapping)
 * matches what runs in dev/prod rather than an in-memory database that
 * would mask PostgreSQL-specific SQL dialect differences.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer("postgres:16");
    }
}
