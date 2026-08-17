package com.edgareldy.springmicroservicestutorial.catalogservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import com.edgareldy.springmicroservicestutorial.catalogservice.mapper.CategoryMapperImpl;
import com.edgareldy.springmicroservicestutorial.catalogservice.repository.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Pure Mockito unit tests for {@link CategoryServiceImpl}: no Spring context,
 * no database, {@link CategoryRepository} is mocked so only this class' own
 * branching logic is exercised (persistence itself is already covered by
 * {@code CategoryRepositoryTest}, backed by Testcontainers PostgreSQL). The
 * MapStruct-generated {@link CategoryMapperImpl} is instantiated for real
 * (not mocked) rather than via {@code @InjectMocks}: mocking a trivial
 * generated mapper would add nothing and would require stubbing every field
 * of every {@code toResponse} call.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, new CategoryMapperImpl());
    }

    @Test
    void create_buildsCategoryFromRequestAndSaves() {
        CategoryRequest request = new CategoryRequest("Books");
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse created = categoryService.create(request);

        assertThat(created.categoryName()).isEqualTo("Books");

        ArgumentCaptor<Category> savedCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getCategoryName()).isEqualTo("Books");
    }

    @Test
    void findAll_delegatesToRepositoryWithSamePageable() {
        Pageable pageable = PageRequest.of(0, 10);
        Category category = Category.builder().id(1L).categoryName("Books").build();
        Page<Category> page = new PageImpl<>(List.of(category), pageable, 1);
        when(categoryRepository.findAll(pageable)).thenReturn(page);

        Page<CategoryResponse> result = categoryService.findAll(pageable);

        assertThat(result.getContent()).containsExactly(new CategoryResponse(1L, "Books"));
        verify(categoryRepository).findAll(pageable);
    }
}
