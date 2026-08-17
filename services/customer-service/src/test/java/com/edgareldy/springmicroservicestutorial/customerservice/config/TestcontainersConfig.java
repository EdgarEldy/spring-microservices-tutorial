package com.edgareldy.springmicroservicestutorial.customerservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Shared Testcontainers configuration for {@code @DataJpaTest} repository
 * tests: starts a single real {@code postgres:16} container per test class
 * and wires it into the Spring context via {@code @ServiceConnection},
 * matching the image used by {@code docker-compose.yml} so behaviour
 * observed here (e.g. the {@code idx_customers_email_lower} case-insensitive
 * uniqueness) matches what runs in dev/prod rather than an in-memory
 * database that would mask PostgreSQL-specific SQL dialect differences.
 * Copied verbatim from {@code auth-service}/{@code catalog-service}'s test
 * config of the same name (never shared via {@code common-lib}, which
 * carries no test code).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
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
