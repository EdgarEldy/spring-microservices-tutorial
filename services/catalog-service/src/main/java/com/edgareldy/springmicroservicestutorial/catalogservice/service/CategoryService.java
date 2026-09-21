package com.edgareldy.springmicroservicestutorial.catalogservice.service;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contract for managing {@link Category} records exposed under
 * {@code /api/v1/catalog/categories}. Every method returns {@link CategoryResponse} directly
 * rather than the {@link Category} entity: nothing outside this service ever needs the raw
 * entity after a call, so there is no reason to make every caller repeat a
 * {@code toResponse(...)} call.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface CategoryService {

    /** Creates a new category. */
    CategoryResponse create(CategoryRequest request);

    /** Returns a page of every category, backing {@code GET /api/v1/catalog/categories}. */
    Page<CategoryResponse> findAll(Pageable pageable);
}
