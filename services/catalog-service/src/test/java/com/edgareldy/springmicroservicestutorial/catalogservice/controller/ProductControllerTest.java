package com.edgareldy.springmicroservicestutorial.catalogservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edgareldy.springmicroservicestutorial.catalogservice.config.MethodSecurityConfig;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Product;
import com.edgareldy.springmicroservicestutorial.catalogservice.security.JwtService;
import com.edgareldy.springmicroservicestutorial.catalogservice.service.ProductService;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc slice tests for {@link ProductController}, with {@link ProductService} mocked. Same
 * pattern as {@link CategoryControllerTest}: servlet filters disabled ({@code addFilters =
 * false}), {@link MethodSecurityConfig} imported so {@code @PreAuthorize("hasRole('ADMIN')")} on
 * {@code create} is genuinely enforced by the method security AOP interceptor, {@code
 * JwtService} mocked purely so {@code JwtAuthFilter} can be constructed.
 * <p>
 * {@code GET /api/v1/catalog/products/{id}} is the endpoint {@code order-service} resolves via
 * OpenFeign to validate/price an order item, so both {@code findByIdReturns200WhenFound} and
 * {@code findByIdReturns404WhenMissing} matter beyond this branch alone: a regression here would
 * only surface downstream, in {@code order-service}'s own {@code WireMock} tests.
 * <p>
 * See {@link CategoryControllerTest}'s class Javadoc for {@code CatalogExceptionHandler}'s
 * validation/access-denied/authentication mappings, shared by every controller in this service;
 * {@code createReturnsForbiddenForNonAdmin} below exercises the access-denied one again for
 * {@code ProductController} specifically.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@WebMvcTest(ProductController.class)
@Import(MethodSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    private static Product product(long id, String name, double price, long categoryId) {
        return Product.builder()
                .id(id)
                .productName(name)
                .unitPrice(price)
                .category(Category.builder().id(categoryId).categoryName("Books").build())
                .build();
    }

    private static ProductResponse response(long id, String name, double price, long categoryId) {
        return new ProductResponse(id, name, price, categoryId);
    }

    @Test
    void findAllWithoutCategoryFilter_isPublicAndReturnsEveryProduct() throws Exception {
        Product cleanCode = product(1L, "Clean Code", 39.90, 1L);
        when(productService.findAll(any(), isNull()))
                .thenReturn(new PageImpl<>(List.of(cleanCode), PageRequest.of(0, 10), 1));
        when(productService.toResponse(cleanCode)).thenReturn(response(1L, "Clean Code", 39.90, 1L));

        mockMvc.perform(get("/api/v1/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].productName").value("Clean Code"));

        verify(productService).findAll(any(), isNull());
    }

    @Test
    void findAllWithCategoryFilter_passesCategoryIdThrough() throws Exception {
        Product cleanCode = product(1L, "Clean Code", 39.90, 1L);
        when(productService.findAll(any(), eq(1L)))
                .thenReturn(new PageImpl<>(List.of(cleanCode), PageRequest.of(0, 10), 1));
        when(productService.toResponse(cleanCode)).thenReturn(response(1L, "Clean Code", 39.90, 1L));

        mockMvc.perform(get("/api/v1/catalog/products").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].categoryId").value(1));

        verify(productService).findAll(any(), eq(1L));
    }

    @Test
    void findByIdReturns200WhenFound() throws Exception {
        Product cleanCode = product(1L, "Clean Code", 39.90, 1L);
        when(productService.findById(1L)).thenReturn(cleanCode);
        when(productService.toResponse(cleanCode)).thenReturn(response(1L, "Clean Code", 39.90, 1L));

        mockMvc.perform(get("/api/v1/catalog/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productName").value("Clean Code"))
                .andExpect(jsonPath("$.data.unitPrice").value(39.90))
                .andExpect(jsonPath("$.data.categoryId").value(1));
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException("No product found with id 99"));

        mockMvc.perform(get("/api/v1/catalog/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturns201ForAdmin() throws Exception {
        ProductRequest request = new ProductRequest("Effective Java", 45.00, 1L);
        Product created = product(2L, "Effective Java", 45.00, 1L);
        when(productService.create(any())).thenReturn(created);
        when(productService.toResponse(created)).thenReturn(response(2L, "Effective Java", 45.00, 1L));

        mockMvc.perform(post("/api/v1/catalog/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productName").value("Effective Java"));
    }

    /**
     * {@code @PreAuthorize} denies this non-admin caller (the service is never invoked);
     * {@code CatalogExceptionHandler.handleAccessDenied} maps the resulting
     * {@code AuthorizationDeniedException} to 403.
     */
    @Test
    @WithMockUser(roles = "USER")
    void createReturnsForbiddenForNonAdmin() throws Exception {
        ProductRequest request = new ProductRequest("Effective Java", 45.00, 1L);

        mockMvc.perform(post("/api/v1/catalog/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(productService, never()).create(any());
    }
}
