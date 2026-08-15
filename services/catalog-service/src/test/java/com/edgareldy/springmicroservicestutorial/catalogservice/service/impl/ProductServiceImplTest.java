package com.edgareldy.springmicroservicestutorial.catalogservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Product;
import com.edgareldy.springmicroservicestutorial.catalogservice.repository.CategoryRepository;
import com.edgareldy.springmicroservicestutorial.catalogservice.repository.ProductRepository;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Pure Mockito unit tests for {@link ProductServiceImpl}: no Spring context,
 * no database, {@link ProductRepository}/{@link CategoryRepository} are
 * mocked so only this class' own branching/mapping logic is exercised
 * (persistence itself is already covered by {@code ProductRepositoryTest},
 * backed by Testcontainers PostgreSQL). {@code findById} is the most
 * important case here: {@code order-service} resolves {@code GET
 * /api/v1/catalog/products/{id}} via OpenFeign to validate/price an order,
 * so both the success path and the {@code ResourceNotFoundException} path
 * (mapped to 404 by {@code order-service}'s Feign error handling) matter.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private static Category category(long id) {
        return Category.builder().id(id).categoryName("Books").build();
    }

    @Test
    void create_existingCategoryId_buildsProductAndSaves() {
        ProductRequest request = new ProductRequest("Clean Code", 39.90, 1L);
        Category category = category(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = productService.create(request);

        assertThat(created.getProductName()).isEqualTo("Clean Code");
        assertThat(created.getUnitPrice()).isEqualTo(39.90);
        assertThat(created.getCategory()).isEqualTo(category);

        ArgumentCaptor<Product> savedCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getCategory().getId()).isEqualTo(1L);
    }

    @Test
    void create_nonExistentCategoryId_throwsResourceNotFoundExceptionAndNeverSaves() {
        ProductRequest request = new ProductRequest("Clean Code", 39.90, 99L);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> productService.create(request));

        verify(productRepository, never()).save(any());
    }

    @Test
    void findAll_categoryIdProvided_delegatesToFindByCategoryId() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = Product.builder().id(1L).category(category(1L)).productName("Clean Code").unitPrice(39.90).build();
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(productRepository.findByCategoryId(1L, pageable)).thenReturn(page);

        Page<Product> result = productService.findAll(pageable, 1L);

        assertThat(result.getContent()).containsExactly(product);
        verify(productRepository).findByCategoryId(1L, pageable);
        verify(productRepository, never()).findAll(pageable);
    }

    @Test
    void findAll_noCategoryId_delegatesToFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = Product.builder().id(1L).category(category(1L)).productName("Clean Code").unitPrice(39.90).build();
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(productRepository.findAll(pageable)).thenReturn(page);

        Page<Product> result = productService.findAll(pageable, null);

        assertThat(result.getContent()).containsExactly(product);
        verify(productRepository).findAll(pageable);
        verify(productRepository, never()).findByCategoryId(any(), any());
    }

    @Test
    void findById_found_returnsProduct() {
        Product product = Product.builder().id(1L).category(category(1L)).productName("Clean Code").unitPrice(39.90).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThat(productService.findById(1L)).isEqualTo(product);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> productService.findById(99L));
    }

    @Test
    void toResponse_mapsFieldsAndFlattensCategoryToPlainId() {
        Product product = Product.builder()
                .id(5L)
                .category(category(2L))
                .productName("Clean Code")
                .unitPrice(39.90)
                .build();

        ProductResponse response = productService.toResponse(product);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.productName()).isEqualTo("Clean Code");
        assertThat(response.unitPrice()).isEqualTo(39.90);
        assertThat(response.categoryId()).isEqualTo(2L);
    }
}
