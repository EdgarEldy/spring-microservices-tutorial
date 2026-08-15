package com.edgareldy.springmicroservicestutorial.authservice.repository;

import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Role}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Eagerly fetches permissions, so a {@link Role} returned here is
     * usable to build authorities without a second lazy-load select.
     */
    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findByRoleNameIgnoreCase(String roleName);

    boolean existsByRoleNameIgnoreCase(String roleName);
}
