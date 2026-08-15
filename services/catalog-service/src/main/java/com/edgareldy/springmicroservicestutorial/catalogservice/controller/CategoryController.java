package com.edgareldy.springmicroservicestutorial.catalogservice.controller;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import com.edgareldy.springmicroservicestutorial.catalogservice.service.CategoryService;
import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.commonlib.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes {@code /api/v1/catalog/categories}: a public paginated listing and
 * an ADMIN-only creation endpoint.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@RestController
@RequestMapping("/api/v1/catalog/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category listing and administration")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "List categories", description = "Public, paginated list of every category")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> findAll(Pageable pageable) {
        Page<Category> page = categoryService.findAll(pageable);
        PageResponse<CategoryResponse> response = PageResponse.of(
                page.getContent().stream().map(categoryService::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(response, "Categories retrieved"));
    }

    @Operation(summary = "Create a category", description = "ADMIN only")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(categoryService.toResponse(category), "Category created"));
    }
}
