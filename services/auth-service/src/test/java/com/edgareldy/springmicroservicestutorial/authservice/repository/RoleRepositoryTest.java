package com.edgareldy.springmicroservicestutorial.authservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.edgareldy.springmicroservicestutorial.authservice.config.TestcontainersConfig;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Repository-level tests for {@link RoleRepository}, backed by a real
 * PostgreSQL container ({@link TestcontainersConfig}) so the
 * case-insensitive functional unique index on {@code role_name} defined in
 * {@code V1__init_schema.sql} and the {@code @EntityGraph} eager fetch of
 * permissions are exercised against actual PostgreSQL behaviour.
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
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void save_and_findByRoleNameIgnoreCase_matchesRegardlessOfCase() {
        roleRepository.save(Role.builder().roleName("Admin").build());

        Optional<Role> found = roleRepository.findByRoleNameIgnoreCase("ADMIN");

        assertThat(found).isPresent();
        assertThat(found.get().getRoleName()).isEqualTo("Admin");
    }

    @Test
    void existsByRoleNameIgnoreCase_trueWhenPresentRegardlessOfCase_falseOtherwise() {
        roleRepository.save(Role.builder().roleName("CUSTOMER").build());

        assertThat(roleRepository.existsByRoleNameIgnoreCase("customer")).isTrue();
        assertThat(roleRepository.existsByRoleNameIgnoreCase("manager")).isFalse();
    }

    @Test
    void save_secondRoleWithSameNameDifferentCase_violatesUniqueIndex() {
        roleRepository.saveAndFlush(Role.builder().roleName("moderator").build());

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> roleRepository.saveAndFlush(Role.builder().roleName("MODERATOR").build()));
    }

    @Test
    void findByRoleNameIgnoreCase_entityGraphLoadsPermissionsEagerly() {
        Permission permission = permissionRepository.save(
                Permission.builder().resource("ORDER").action("READ").build());
        roleRepository.save(Role.builder().roleName("support").permissions(Set.of(permission)).build());

        // Flush + clear forces the next read through a fresh Hibernate
        // session/persistence context, proving the @EntityGraph fetch (not
        // an already-loaded first-level cache entry) supplies permissions.
        entityManager.flush();
        entityManager.clear();

        Role reloaded = roleRepository.findByRoleNameIgnoreCase("SUPPORT").orElseThrow();

        // No LazyInitializationException expected: permissions must already
        // be initialized by the @EntityGraph on findByRoleNameIgnoreCase.
        assertThat(reloaded.getPermissions())
                .extracting(Permission::getResource, Permission::getAction)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("ORDER", "READ"));
    }
}
