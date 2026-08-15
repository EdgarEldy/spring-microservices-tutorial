package com.edgareldy.springmicroservicestutorial.authservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmicroservicestutorial.authservice.config.TestcontainersConfig;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Repository-level tests for {@link PermissionRepository}, backed by a real
 * PostgreSQL container ({@link TestcontainersConfig}) verifying that the
 * resource/action pair lookup is case-insensitive on both columns.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@DataJpaTest
// Spring Boot 4.x's @DataJpaTest slice no longer imports FlywayAutoConfiguration by
// default (unlike 3.x): without this, the schema created by V1__init_schema.sql would
// never be applied against the Testcontainers database and every query below would fail
// with "relation does not exist".
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(TestcontainersConfig.class)
class PermissionRepositoryTest {

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    void existsByResourceIgnoreCaseAndActionIgnoreCase_trueWhenSamePairRegardlessOfCase() {
        permissionRepository.save(Permission.builder().resource("PRODUCT").action("write").build());

        assertThat(permissionRepository.existsByResourceIgnoreCaseAndActionIgnoreCase("product", "WRITE"))
                .isTrue();
    }

    @Test
    void existsByResourceIgnoreCaseAndActionIgnoreCase_falseWhenPairDoesNotMatch() {
        permissionRepository.save(Permission.builder().resource("PRODUCT").action("WRITE").build());

        assertThat(permissionRepository.existsByResourceIgnoreCaseAndActionIgnoreCase("product", "READ"))
                .isFalse();
        assertThat(permissionRepository.existsByResourceIgnoreCaseAndActionIgnoreCase("order", "WRITE"))
                .isFalse();
    }
}
