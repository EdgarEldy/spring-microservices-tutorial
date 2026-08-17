package com.edgareldy.springmicroservicestutorial.catalogservice.service.impl;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Product;
import com.edgareldy.springmicroservicestutorial.catalogservice.mapper.ProductMapper;
import com.edgareldy.springmicroservicestutorial.catalogservice.repository.ProductRepository;
import com.edgareldy.springmicroservicestutorial.catalogservice.service.ProductService;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link ProductService} implementation, backed by {@link ProductRepository} and
 * delegating entity-DTO mapping in both directions, including {@code categoryId} resolution,
 * to {@link ProductMapper}. Every method maps its entity to a {@link ProductResponse} before
 * returning, since nothing else needs the raw {@link Product} afterward.
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
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable, Long categoryId) {
        Page<Product> page = categoryId != null
                ? productRepository.findByCategoryId(categoryId, pageable)
                : productRepository.findAll(pageable);
        return page.map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No product found with id " + id));
    }
}
