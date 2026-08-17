package com.edgareldy.springmicroservicestutorial.authservice.repository;

import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link User}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Eagerly fetches roles and their permissions, so a {@link User}
     * returned by this method has a fully usable
     * {@link User#getAuthorities()} even after the transaction/session that
     * loaded it has closed (e.g. once handed back to a JWT filter).
     */
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
