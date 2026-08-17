package com.edgareldy.springmicroservicestutorial.customerservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springmicroservicestutorial.customerservice.config.TestcontainersConfig;
import com.edgareldy.springmicroservicestutorial.customerservice.entity.Customer;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Repository-level tests for {@link CustomerRepository}, backed by a real
 * PostgreSQL container ({@link TestcontainersConfig}) rather than an
 * in-memory database, so the {@code customers} table created by
 * {@code V1__init_schema.sql} (including its case-insensitive
 * {@code idx_customers_email_lower} unique index) is exercised against
 * actual PostgreSQL behaviour. {@link #customerEntity_hasNoForeignKeyOrAssociationToAnotherServicesSchema()}
 * backs the README's "confirms customer-service never attempts a direct
 * database call against auth-service's schema" requirement: {@code userId}
 * must stay a plain column forever, never a JPA association Hibernate would
 * try to resolve by querying another service's database.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@DataJpaTest
// Spring Boot 4.x's @DataJpaTest slice no longer imports FlywayAutoConfiguration by
// default: without this, V1__init_schema.sql never runs against the Testcontainers
// database and every query below fails with "relation does not exist".
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(TestcontainersConfig.class)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    private static Customer newCustomer(String email) {
        return Customer.builder()
                .userId(1L)
                .firstName("Ada")
                .lastName("Lovelace")
                .telephone("+1234567890")
                .email(email)
                .address("123 Main St")
                .build();
    }

    @Test
    void save_persistsCustomerWithGeneratedId() {
        Customer saved = customerRepository.save(newCustomer("ada@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void findByEmailIgnoreCase_matchesRegardlessOfCase() {
        customerRepository.save(newCustomer("Ada@Example.com"));

        Optional<Customer> found = customerRepository.findByEmailIgnoreCase("ada@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("Ada@Example.com");
    }

    @Test
    void existsByEmailIgnoreCase_matchesRegardlessOfCase() {
        customerRepository.save(newCustomer("Ada@Example.com"));

        assertThat(customerRepository.existsByEmailIgnoreCase("ada@example.com")).isTrue();
        assertThat(customerRepository.existsByEmailIgnoreCase("nobody@example.com")).isFalse();
    }

    @Test
    void save_duplicateEmailDifferentCase_violatesUniqueIndex() {
        customerRepository.save(newCustomer("Ada@Example.com"));
        customerRepository.flush();

        assertThatThrownBy(() -> {
                    customerRepository.save(newCustomer("ada@example.com"));
                    customerRepository.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void customerEntity_hasNoForeignKeyOrAssociationToAnotherServicesSchema() {
        for (Field field : Customer.class.getDeclaredFields()) {
            assertThat(field.isAnnotationPresent(ManyToOne.class))
                    .as("%s must never be a @ManyToOne, userId stays a plain column", field.getName())
                    .isFalse();
            assertThat(field.isAnnotationPresent(OneToOne.class))
                    .as("%s must never be a @OneToOne, userId stays a plain column", field.getName())
                    .isFalse();
            assertThat(field.isAnnotationPresent(JoinColumn.class))
                    .as("%s must never carry a @JoinColumn, no FK crosses into auth_db", field.getName())
                    .isFalse();
        }
    }
}
