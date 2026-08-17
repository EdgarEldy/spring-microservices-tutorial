package com.edgareldy.springmicroservicestutorial.catalogservice.repository;

import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Category}. {@code Page<Category>
 * findAll(Pageable)}, needed for {@code GET /api/v1/catalog/categories}'s
 * pagination, is already inherited from {@link JpaRepository} and needs no
 * extra declaration here.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
