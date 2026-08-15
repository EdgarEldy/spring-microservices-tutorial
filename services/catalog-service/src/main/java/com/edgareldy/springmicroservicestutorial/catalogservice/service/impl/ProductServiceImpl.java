package com.edgareldy.springmicroservicestutorial.catalogservice.service.impl;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Product;
import com.edgareldy.springmicroservicestutorial.catalogservice.repository.CategoryRepository;
import com.edgareldy.springmicroservicestutorial.catalogservice.repository.ProductRepository;
import com.edgareldy.springmicroservicestutorial.catalogservice.service.ProductService;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link ProductService} implementation, backed by {@link ProductRepository}
 * and {@link CategoryRepository} (to validate {@code categoryId} on creation).
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public Product create(ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("No category found with id " + request.categoryId()));
        Product product = Product.builder()
                .category(category)
                .productName(request.productName())
                .unitPrice(request.unitPrice())
                .build();
        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findAll(Pageable pageable, Long categoryId) {
        return categoryId != null
                ? productRepository.findByCategoryId(categoryId, pageable)
                : productRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No product found with id " + id));
    }

    @Override
    public ProductResponse toResponse(Product product) {
        // getCategory().getId() reads the id off a (possibly lazy) proxy, which Hibernate
        // resolves without a second query since the FK value is already held by the proxy.
        return new ProductResponse(
                product.getId(), product.getProductName(), product.getUnitPrice(), product.getCategory().getId());
    }
}
