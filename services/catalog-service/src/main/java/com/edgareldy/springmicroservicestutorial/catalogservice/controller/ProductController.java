package com.edgareldy.springmicroservicestutorial.catalogservice.controller;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Product;
import com.edgareldy.springmicroservicestutorial.catalogservice.service.ProductService;
import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.commonlib.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes {@code /api/v1/catalog/products}: a public paginated listing
 * (optionally filtered by {@code categoryId}), a public detail endpoint (the
 * one {@code order-service} resolves via OpenFeign, later formalized by
 * {@code feature/contract-testing}, so its response shape must stay stable),
 * and an ADMIN-only creation endpoint.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product listing, detail lookup and administration")
public class ProductController {

    private final ProductService productService;

    @Operation(
            summary = "List products",
            description = "Public, paginated list of every product, optionally filtered by categoryId")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> findAll(
            Pageable pageable, @Parameter(description = "Optional category filter") @RequestParam(required = false) Long categoryId) {
        Page<Product> page = productService.findAll(pageable, categoryId);
        PageResponse<ProductResponse> response = PageResponse.of(
                page.getContent().stream().map(productService::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(response, "Products retrieved"));
    }

    @Operation(
            summary = "Get a product by id",
            description = "Public. Called by order-service via OpenFeign to validate/price order items; "
                    + "response shape is formalized later by feature/contract-testing")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> findById(@PathVariable Long id) {
        Product product = productService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(productService.toResponse(product), "Product retrieved"));
    }

    @Operation(summary = "Create a product", description = "ADMIN only")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        Product product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productService.toResponse(product), "Product created"));
    }
}
