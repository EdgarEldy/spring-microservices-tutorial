package com.edgareldy.springmicroservicestutorial.catalogservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import com.edgareldy.springmicroservicestutorial.catalogservice.repository.CategoryRepository;
import java.util.List;
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
 * Pure Mockito unit tests for {@link CategoryServiceImpl}: no Spring context,
 * no database, {@link CategoryRepository} is mocked so only this class' own
 * branching/mapping logic is exercised (persistence itself is already
 * covered by {@code CategoryRepositoryTest}, backed by Testcontainers
 * PostgreSQL).
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

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void create_buildsCategoryFromRequestAndSaves() {
        CategoryRequest request = new CategoryRequest("Books");
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category created = categoryService.create(request);

        assertThat(created.getCategoryName()).isEqualTo("Books");

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

        Page<Category> result = categoryService.findAll(pageable);

        assertThat(result.getContent()).containsExactly(category);
        verify(categoryRepository).findAll(pageable);
    }

    @Test
    void toResponse_mapsIdAndCategoryName() {
        Category category = Category.builder().id(7L).categoryName("Electronics").build();

        CategoryResponse response = categoryService.toResponse(category);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.categoryName()).isEqualTo("Electronics");
    }
}
