package com.edgareldy.springmicroservicestutorial.catalogservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edgareldy.springmicroservicestutorial.catalogservice.config.MethodSecurityConfig;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.CategoryResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.security.JwtService;
import com.edgareldy.springmicroservicestutorial.catalogservice.service.CategoryService;
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
 * MockMvc slice tests for {@link CategoryController}, with {@link CategoryService} mocked.
 * Servlet filters are disabled ({@code addFilters = false}), but {@link MethodSecurityConfig} is
 * imported so {@code @PreAuthorize("hasRole('ADMIN')")} on {@code create} is genuinely enforced
 * by Spring's method security AOP interceptor, independently of the servlet filter chain
 * ({@code SecurityConfig}'s own {@code hasRole('ADMIN')} rule for {@code POST
 * /api/v1/catalog/**} never runs here). {@code JwtService} is mocked purely so {@code
 * JwtAuthFilter} (auto-included as a {@code Filter} bean by {@code @WebMvcTest} regardless of
 * {@code addFilters}) can be constructed; unlike {@code auth-service}, there is no {@code
 * UserDetailsService}/{@code BlacklistedTokenRepository} to mock here, since {@code
 * catalog-service} owns no user data.
 * <p>
 * {@code CatalogExceptionHandler} maps {@code MethodArgumentNotValidException} to 400,
 * {@code AccessDeniedException} (and its subtype {@code AuthorizationDeniedException}, thrown by
 * a {@code @PreAuthorize} denial) to 403, and the generic {@code AuthenticationException}
 * (thrown when {@code hasRole('ADMIN')} is evaluated with no {@code Authentication} at all) to
 * 401, mirroring {@code auth-service}'s {@code AuthExceptionHandler}. The tests below assert
 * those codes, not the 500 a missing handler would have produced.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@WebMvcTest(CategoryController.class)
@Import(MethodSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtService jwtService;

    private static CategoryResponse response(long id, String name) {
        return new CategoryResponse(id, name);
    }

    @Test
    void findAllIsPublicAndReturnsPagedApiResponse() throws Exception {
        when(categoryService.findAll(any()))
                .thenReturn(new PageImpl<>(List.of(response(1L, "Books")), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].categoryName").value("Books"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturns201ForAdmin() throws Exception {
        CategoryRequest request = new CategoryRequest("Garden");
        when(categoryService.create(any())).thenReturn(response(2L, "Garden"));

        mockMvc.perform(post("/api/v1/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categoryName").value("Garden"));
    }

    /**
     * {@code @PreAuthorize("hasRole('ADMIN')")} denies a non-admin caller (the service is never
     * invoked); {@code CatalogExceptionHandler.handleAccessDenied} maps the resulting
     * {@code AuthorizationDeniedException} to 403.
     */
    @Test
    @WithMockUser(roles = "USER")
    void createReturnsForbiddenForNonAdmin() throws Exception {
        CategoryRequest request = new CategoryRequest("Garden");

        mockMvc.perform(post("/api/v1/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(categoryService, never()).create(any());
    }

    /**
     * No {@code Authentication} at all denies the request too (the service is never invoked);
     * {@code CatalogExceptionHandler.handleAuthentication} maps the resulting
     * {@code AuthenticationCredentialsNotFoundException} to 401.
     */
    @Test
    void createReturnsUnauthorizedWithoutAuthentication() throws Exception {
        CategoryRequest request = new CategoryRequest("Garden");

        mockMvc.perform(post("/api/v1/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verify(categoryService, never()).create(any());
    }

    /**
     * {@code @Valid} rejects the blank {@code categoryName} before the controller body ever
     * runs (the service is never invoked); {@code CatalogExceptionHandler.handleValidation}
     * maps the resulting {@code MethodArgumentNotValidException} to 400.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void createRejectsInvalidPayload() throws Exception {
        CategoryRequest blankName = new CategoryRequest(" ");

        mockMvc.perform(post("/api/v1/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(categoryService, never()).create(any());
    }
}
