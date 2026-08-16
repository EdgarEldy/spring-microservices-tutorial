package com.edgareldy.springmicroservicestutorial.catalogservice.mapper;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Product;
import com.edgareldy.springmicroservicestutorial.catalogservice.repository.CategoryRepository;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MapStruct mapper translating between {@link Product} entities and their request/response
 * DTOs. Declared as an abstract class rather than an interface (unlike {@link CategoryMapper})
 * because {@code toEntity(ProductRequest)} needs a real collaborator, {@link CategoryRepository},
 * injected as a protected field the way MapStruct expects extra dependencies to be wired into a
 * generated implementation.
 * <p>
 * {@code toResponse}: {@code id}/{@code productName}/{@code unitPrice} map field-for-field, and
 * {@code categoryId} is read off the nested {@code product.category.id} path via
 * {@code @Mapping(source = "category.id")}, a plain nested-property read MapStruct resolves on
 * its own, no qualifier needed.
 * <p>
 * {@code toEntity(ProductRequest)}: {@code productName}/{@code unitPrice} map directly,
 * {@code id} is ignored (left null for a new, unsaved entity), and {@code category} is resolved
 * from {@code request.categoryId()} via {@link #resolveCategory(Long)}, which now owns the
 * existence-validation this used to be {@code ProductServiceImpl.create}'s job.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Mapper(componentModel = "spring")
public abstract class ProductMapper {

    @Autowired
    protected CategoryRepository categoryRepository;

    @Mapping(target = "categoryId", source = "category.id")
    public abstract ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "resolveCategory")
    public abstract Product toEntity(ProductRequest request);

    /**
     * Looks up the category a new product must belong to, throwing
     * {@link ResourceNotFoundException} when {@code categoryId} matches no existing
     * {@link Category}; referenced from {@link #toEntity(ProductRequest)} via
     * {@code qualifiedByName} rather than inlined, so the lookup has a single, named,
     * testable entry point.
     */
    @Named("resolveCategory")
    protected Category resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("No category found with id " + categoryId));
    }
}
