package com.edgareldy.springmicroservicestutorial.catalogservice.service.impl;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import com.edgareldy.springmicroservicestutorial.catalogservice.repository.CategoryRepository;
import com.edgareldy.springmicroservicestutorial.catalogservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link CategoryService} implementation, backed by {@link CategoryRepository}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public Category create(CategoryRequest request) {
        Category category = Category.builder().categoryName(request.categoryName()).build();
        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Category> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    @Override
    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getCategoryName());
    }
}
