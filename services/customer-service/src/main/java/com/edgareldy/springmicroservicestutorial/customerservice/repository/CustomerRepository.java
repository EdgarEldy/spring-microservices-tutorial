package com.edgareldy.springmicroservicestutorial.customerservice.repository;

import com.edgareldy.springmicroservicestutorial.customerservice.entity.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Customer}. {@code Page<Customer>
 * findAll(Pageable)} is already inherited from {@link JpaRepository} and
 * needs no extra declaration here. Every query below stays scoped to
 * {@code customer_db}'s own {@code customers} table: nothing in this
 * repository (or anywhere else in this service) ever queries
 * {@code auth-service}'s {@code users} table directly, matching
 * {@link Customer}'s plain, non-FK {@code userId} column.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Backs the case-insensitive unique index (idx_customers_email_lower)
    // created in V1__init_schema.sql, same pattern as auth-service's
    // UserRepository.findByEmailIgnoreCase.
    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
