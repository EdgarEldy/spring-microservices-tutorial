package com.edgareldy.springmicroservicestutorial.orderservice.repository;

import com.edgareldy.springmicroservicestutorial.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Order}. {@code Page<Order> findAll(Pageable)} is
 * already inherited from {@link JpaRepository} and needs no extra declaration here.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
