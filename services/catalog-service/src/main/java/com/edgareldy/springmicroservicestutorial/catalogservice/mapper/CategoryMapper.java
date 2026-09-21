package com.edgareldy.springmicroservicestutorial.catalogservice.mapper;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper translating between {@link Category} entities and their request/response
 * DTOs. Both directions are pure field-for-field mappings ({@code id}/{@code categoryName} for
 * {@code toResponse}; {@code categoryName} for {@code toEntity}, {@code id} left null for a new,
 * unsaved entity), so no explicit {@code @Mapping} is needed beyond ignoring {@code id} on
 * creation.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    @Mapping(target = "id", ignore = true)
    Category toEntity(CategoryRequest request);
}
