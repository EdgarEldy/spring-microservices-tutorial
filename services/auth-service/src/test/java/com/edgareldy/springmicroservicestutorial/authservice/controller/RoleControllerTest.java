package com.edgareldy.springmicroservicestutorial.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edgareldy.springmicroservicestutorial.authservice.config.MethodSecurityConfig;
import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import com.edgareldy.springmicroservicestutorial.authservice.repository.BlacklistedTokenRepository;
import com.edgareldy.springmicroservicestutorial.authservice.security.CustomPermissionEvaluator;
import com.edgareldy.springmicroservicestutorial.authservice.security.JwtService;
import com.edgareldy.springmicroservicestutorial.authservice.security.UserDetailsServiceImpl;
import com.edgareldy.springmicroservicestutorial.authservice.service.RoleService;
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
 * MockMvc slice tests for {@link RoleController}, with {@link RoleService} mocked. Servlet
 * filters are disabled ({@code addFilters = false}), but {@link MethodSecurityConfig} (plus its
 * {@link CustomPermissionEvaluator} dependency) is imported so the class-level
 * {@code @PreAuthorize("hasRole('ADMIN')")} is genuinely enforced by Spring's method security AOP
 * interceptor. {@code JwtService}, {@code BlacklistedTokenRepository} and
 * {@code UserDetailsServiceImpl} are mocked purely so {@code JwtAuthFilter} (auto-included as a
 * {@code Filter} bean by {@code @WebMvcTest} regardless of {@code addFilters}) can be
 * constructed.
 * <p>
 * A {@code @PreAuthorize} denial thrown by the method-security AOP interceptor around a
 * controller method is caught by {@code DispatcherServlet}'s own
 * {@code ExceptionHandlerExceptionResolver} (which inspects {@code @ControllerAdvice} beans)
 * before it can ever reach {@code SecurityConfig}'s
 * {@code ExceptionTranslationFilter}/{@code CustomAccessDeniedHandler}, so
 * {@code AuthExceptionHandler.handleAccessDenied} is what actually maps it to 403 here, not that
 * filter-level handler. The {@code *ReturnsForbiddenForNonAdmin} tests below assert that 403,
 * while also verifying the service method is never invoked.
 * <p>
 * A fully unauthenticated request (no {@code @WithMockUser} at all) takes a different path: with
 * no {@code Authentication} in the {@code SecurityContext}, {@code hasRole('ADMIN')} evaluation
 * throws {@code AuthenticationCredentialsNotFoundException} rather than
 * {@code AccessDeniedException}, which {@code AuthExceptionHandler.handleAuthentication} does map
 * to 401 (see {@link #findAllReturnsUnauthorizedWithoutAuthentication()}).
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@WebMvcTest(RoleController.class)
@Import({MethodSecurityConfig.class, CustomPermissionEvaluator.class})
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private static Role role(long id, String name) {
        return Role.builder().id(id).roleName(name).build();
    }

    private static RoleResponse response(long id, String name) {
        return new RoleResponse(id, name, List.of());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findAllReturns200ForAdmin() throws Exception {
        when(roleService.findAll()).thenReturn(List.of(role(1L, "ADMIN")));
        when(roleService.toResponse(any())).thenReturn(response(1L, "ADMIN"));

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].roleName").value("ADMIN"));
    }

    @Test
    void findAllReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isUnauthorized());

        verify(roleService, never()).findAll();
    }

    @Test
    @WithMockUser(roles = "USER")
    void findAllReturnsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(roleService, never()).findAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturns201ForAdmin() throws Exception {
        RoleRequest request = new RoleRequest("MODERATOR");
        Role created = role(2L, "MODERATOR");
        when(roleService.create(any())).thenReturn(created);
        when(roleService.toResponse(created)).thenReturn(response(2L, "MODERATOR"));

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.roleName").value("MODERATOR"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createReturnsForbiddenForNonAdmin() throws Exception {
        RoleRequest request = new RoleRequest("MODERATOR");

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(roleService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturns204ForAdminWithNoBody() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/1"))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        verify(roleService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteReturnsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(roleService, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturns404WhenMissing() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Role not found with id 99"))
                .when(roleService).delete(99L);

        mockMvc.perform(delete("/api/v1/roles/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addPermissionReturns200ForAdmin() throws Exception {
        Role updated = role(1L, "ADMIN");
        when(roleService.addPermission(1L, 2L)).thenReturn(updated);
        when(roleService.toResponse(updated)).thenReturn(response(1L, "ADMIN"));

        mockMvc.perform(post("/api/v1/roles/1/permissions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Permission granted"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void addPermissionReturnsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/roles/1/permissions/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(roleService, never()).addPermission(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void removePermissionReturns200ForAdmin() throws Exception {
        Role updated = role(1L, "ADMIN");
        when(roleService.removePermission(1L, 2L)).thenReturn(updated);
        when(roleService.toResponse(updated)).thenReturn(response(1L, "ADMIN"));

        mockMvc.perform(delete("/api/v1/roles/1/permissions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Permission revoked"));

        verify(roleService).removePermission(eq(1L), eq(2L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void removePermissionReturnsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/1/permissions/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(roleService, never()).removePermission(any(), any());
    }
}
