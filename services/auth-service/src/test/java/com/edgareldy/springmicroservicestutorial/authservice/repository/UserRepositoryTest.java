package com.edgareldy.springmicroservicestutorial.authservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.edgareldy.springmicroservicestutorial.authservice.config.TestcontainersConfig;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
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
 * Repository-level tests for {@link UserRepository}, backed by a real
 * PostgreSQL container ({@link TestcontainersConfig}) so the case-insensitive
 * functional unique index defined in {@code V1__init_schema.sql} and the
 * {@code @EntityGraph} eager fetch of roles/permissions are both exercised
 * against actual PostgreSQL behaviour rather than an in-memory database.
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
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private EntityManager entityManager;

    private User newUser(String email) {
        return User.builder()
                .firstName("Ada")
                .lastName("Lovelace")
                .email(email)
                .password("hashed-password")
                .enabled(true)
                .accountLocked(false)
                .build();
    }

    @Test
    void save_and_findByEmailIgnoreCase_matchesRegardlessOfCase() {
        userRepository.save(newUser("Ada.Lovelace@Example.com"));

        Optional<User> found = userRepository.findByEmailIgnoreCase("ada.lovelace@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("Ada.Lovelace@Example.com");
    }

    @Test
    void existsByEmailIgnoreCase_trueWhenPresentRegardlessOfCase_falseOtherwise() {
        userRepository.save(newUser("bob@example.com"));

        assertThat(userRepository.existsByEmailIgnoreCase("BOB@EXAMPLE.COM")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("nobody@example.com")).isFalse();
    }

    @Test
    void save_secondUserWithSameEmailDifferentCase_violatesUniqueIndex() {
        userRepository.saveAndFlush(newUser("carol@example.com"));

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> userRepository.saveAndFlush(newUser("Carol@Example.com")));
    }

    @Test
    void findByEmailIgnoreCase_entityGraphLoadsAuthoritiesEagerlyAfterSessionDetach() {
        Permission permission = permissionRepository.save(
                Permission.builder().resource("PRODUCT").action("WRITE").build());
        Role role = roleRepository.save(
                Role.builder().roleName("ADMIN").permissions(Set.of(permission)).build());
        User user = newUser("dave@example.com");
        user.setRoles(Set.of(role));
        userRepository.save(user);

        // Flush + clear forces the next read to go through a fresh Hibernate
        // session/persistence context, proving the @EntityGraph fetch (not
        // an already-loaded first-level cache entry) supplies roles and
        // permissions.
        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findByEmailIgnoreCase("dave@example.com").orElseThrow();

        // No LazyInitializationException expected here: the roles and
        // roles.permissions collections must already be initialized by the
        // @EntityGraph on findByEmailIgnoreCase, since no open session is
        // guaranteed once this method returns to a caller (e.g. a JWT
        // filter running outside a transaction).
        assertThat(reloaded.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "PERMISSION_PRODUCT:WRITE");
    }
}
