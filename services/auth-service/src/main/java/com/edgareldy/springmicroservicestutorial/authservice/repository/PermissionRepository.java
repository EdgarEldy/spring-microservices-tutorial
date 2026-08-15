package com.edgareldy.springmicroservicestutorial.authservice.repository;

import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Permission}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByResourceIgnoreCaseAndActionIgnoreCase(String resource, String action);
}
