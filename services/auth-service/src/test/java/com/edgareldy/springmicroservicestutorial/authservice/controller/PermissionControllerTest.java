package com.edgareldy.springmicroservicestutorial.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edgareldy.springmicroservicestutorial.authservice.config.MethodSecurityConfig;
import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionResponse;
import com.edgareldy.springmicroservicestutorial.authservice.repository.BlacklistedTokenRepository;
import com.edgareldy.springmicroservicestutorial.authservice.security.CustomPermissionEvaluator;
import com.edgareldy.springmicroservicestutorial.authservice.security.JwtService;
import com.edgareldy.springmicroservicestutorial.authservice.security.UserDetailsServiceImpl;
import com.edgareldy.springmicroservicestutorial.authservice.service.PermissionService;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc slice tests for {@link PermissionController}, with {@link PermissionService} mocked.
 * Servlet filters are disabled ({@code addFilters = false}), but {@link MethodSecurityConfig}
 * (plus its {@link CustomPermissionEvaluator} dependency) is imported so the class-level
 * {@code @PreAuthorize("hasRole('ADMIN')")} is genuinely enforced by Spring's method security AOP
 * interceptor. {@code JwtService}, {@code BlacklistedTokenRepository} and
 * {@code UserDetailsServiceImpl} are mocked purely so {@code JwtAuthFilter} (auto-included as a
 * {@code Filter} bean by {@code @WebMvcTest} regardless of {@code addFilters}) can be
 * constructed.
 * <p>
 * {@code DispatcherServlet}'s own {@code ExceptionHandlerExceptionResolver} resolves a
 * {@code @PreAuthorize} denial before {@code SecurityConfig}'s filter-level
 * {@code CustomAccessDeniedHandler} ever sees it, so {@code AuthExceptionHandler.handleAccessDenied}
 * is what actually maps it to 403 here. The {@code *ReturnsForbiddenForNonAdmin} tests below assert
 * that 403, while also verifying the service method is never invoked. A fully unauthenticated
 * request instead throws {@code AuthenticationCredentialsNotFoundException}
 * (no {@code Authentication} at all to evaluate {@code hasRole('ADMIN')} against), which
 * {@code AuthExceptionHandler.handleAuthentication} does map to 401.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@WebMvcTest(PermissionController.class)
@Import({MethodSecurityConfig.class, CustomPermissionEvaluator.class})
@AutoConfigureMockMvc(addFilters = false)
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private static PermissionResponse response(long id, String resource, String action) {
        return new PermissionResponse(id, resource, action);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findAllReturns200ForAdmin() throws Exception {
        when(permissionService.findAll()).thenReturn(List.of(response(1L, "PRODUCT", "READ")));

        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].resource").value("PRODUCT"));
    }

    @Test
    void findAllReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isUnauthorized());

        verify(permissionService, never()).findAll();
    }

    @Test
    @WithMockUser(roles = "USER")
    void findAllReturnsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(permissionService, never()).findAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturns201ForAdmin() throws Exception {
        PermissionRequest request = new PermissionRequest("ORDER", "WRITE");
        when(permissionService.create(any())).thenReturn(response(2L, "ORDER", "WRITE"));

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.resource").value("ORDER"))
                .andExpect(jsonPath("$.data.action").value("WRITE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createReturnsForbiddenForNonAdmin() throws Exception {
        PermissionRequest request = new PermissionRequest("ORDER", "WRITE");

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(permissionService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturns200ForAdminWithApiResponseBody() throws Exception {
        mockMvc.perform(delete("/api/v1/permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(permissionService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteReturnsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/permissions/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(permissionService, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturns404WhenMissing() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Permission not found with id 99"))
                .when(permissionService).delete(99L);

        mockMvc.perform(delete("/api/v1/permissions/99"))
                .andExpect(status().isNotFound());
    }
}
