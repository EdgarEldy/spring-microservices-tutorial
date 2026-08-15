package com.edgareldy.springmicroservicestutorial.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edgareldy.springmicroservicestutorial.authservice.config.MethodSecurityConfig;
import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UpdateProfileRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UserResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import com.edgareldy.springmicroservicestutorial.authservice.repository.BlacklistedTokenRepository;
import com.edgareldy.springmicroservicestutorial.authservice.security.CustomPermissionEvaluator;
import com.edgareldy.springmicroservicestutorial.authservice.security.JwtService;
import com.edgareldy.springmicroservicestutorial.authservice.security.UserDetailsServiceImpl;
import com.edgareldy.springmicroservicestutorial.authservice.service.UserService;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc slice tests for {@link UserController}, with {@link UserService} mocked. Servlet
 * filters are disabled ({@code addFilters = false}), but {@link MethodSecurityConfig} (plus its
 * {@link CustomPermissionEvaluator} dependency) is imported so
 * {@code @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")} is genuinely
 * enforced by Spring's method security AOP interceptor, which runs independently of the servlet
 * filter chain. {@code JwtService}, {@code BlacklistedTokenRepository} and
 * {@code UserDetailsServiceImpl} are mocked purely so {@code JwtAuthFilter} (auto-included as a
 * {@code Filter} bean by {@code @WebMvcTest} regardless of {@code addFilters}) can be
 * constructed.
 * <p>
 * <strong>Technical choice for the ownership check</strong>: {@code @WithMockUser}'s default
 * principal is Spring Security's own {@code org.springframework.security.core.userdetails.User},
 * which has no {@code id} property, so evaluating {@code authentication.principal.id} against it
 * throws rather than simply denying. The {@code #id == authentication.principal.id} branch of
 * the expression is only reached once the {@code hasRole('ADMIN')} branch is false (SpEL
 * {@code or} short-circuits), which is exactly the owner/non-owner tests below. Those tests
 * therefore bypass {@code @WithMockUser} and instead push a real
 * {@link com.edgareldy.springmicroservicestutorial.authservice.entity.User} entity (which does
 * expose {@code getId()}) as the principal directly into {@link SecurityContextHolder} via a
 * small {@code authenticateAs} helper, cleared in {@link #clearSecurityContext()} after every
 * test. The admin tests keep using {@code @WithMockUser(roles = "ADMIN")} since the
 * short-circuited {@code or} never touches {@code principal.id} in that case.
 * <p>
 * {@code AuthExceptionHandler.handleAccessDenied} maps a {@code @PreAuthorize} denial (here, the
 * {@code or} expression evaluating to false for a non-admin, non-owner caller) to a 403, so the
 * {@code *ReturnsForbiddenForNonAdminNonOwner} tests below assert that, while also verifying the
 * service method is never invoked.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@WebMvcTest(UserController.class)
@Import({MethodSecurityConfig.class, CustomPermissionEvaluator.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Pushes the given entity directly as the authenticated principal, see class Javadoc. */
    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static User userWithId(long id) {
        return User.builder()
                .id(id)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("user" + id + "@example.com")
                .enabled(true)
                .accountLocked(false)
                .roles(Set.of())
                .build();
    }

    private static UserResponse response(long id) {
        return new UserResponse(id, "Ada", "Lovelace", "user" + id + "@example.com", true, false, java.util.List.of());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturns200ForAdmin() throws Exception {
        User user = userWithId(1L);
        when(userService.findById(1L)).thenReturn(user);
        when(userService.toResponse(user)).thenReturn(response(1L));

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getByIdReturns200ForOwner() throws Exception {
        authenticateAs(userWithId(1L));
        User user = userWithId(1L);
        when(userService.findById(1L)).thenReturn(user);
        when(userService.toResponse(user)).thenReturn(response(1L));

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getByIdReturnsForbiddenForNonAdminNonOwner() throws Exception {
        authenticateAs(userWithId(2L));

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(userService, never()).findById(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProfileReturns200ForAdmin() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Ada", "Byron");
        User updated = userWithId(1L);
        when(userService.updateProfile(eq(1L), any())).thenReturn(updated);
        when(userService.toResponse(updated)).thenReturn(response(1L));

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void updateProfileReturns200ForOwner() throws Exception {
        authenticateAs(userWithId(1L));
        UpdateProfileRequest request = new UpdateProfileRequest("Ada", "Byron");
        User updated = userWithId(1L);
        when(userService.updateProfile(eq(1L), any())).thenReturn(updated);
        when(userService.toResponse(updated)).thenReturn(response(1L));

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void updateProfileReturnsForbiddenForNonAdminNonOwner() throws Exception {
        authenticateAs(userWithId(2L));
        UpdateProfileRequest request = new UpdateProfileRequest("Ada", "Byron");

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProfileReturns404WhenUserMissing() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Ada", "Byron");
        when(userService.updateProfile(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("User not found with id 99"));

        mockMvc.perform(put("/api/v1/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
