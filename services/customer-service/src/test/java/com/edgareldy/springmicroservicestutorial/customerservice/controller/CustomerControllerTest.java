package com.edgareldy.springmicroservicestutorial.customerservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import com.edgareldy.springmicroservicestutorial.customerservice.config.SecurityConfig;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerResponse;
import com.edgareldy.springmicroservicestutorial.customerservice.dto.CustomerUpdateRequest;
import com.edgareldy.springmicroservicestutorial.customerservice.security.CustomAccessDeniedHandler;
import com.edgareldy.springmicroservicestutorial.customerservice.security.CustomAuthenticationEntryPoint;
import com.edgareldy.springmicroservicestutorial.customerservice.security.JwtService;
import com.edgareldy.springmicroservicestutorial.customerservice.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc slice tests for {@link CustomerController}, with {@link CustomerService} mocked.
 * Unlike {@code catalog-service}'s controller tests, servlet filters are left enabled
 * (no {@code addFilters = false}) since there is no {@code @PreAuthorize}/{@code
 * MethodSecurityConfig} in this service: every {@code /api/v1/customers/**} route requires
 * only {@code authenticated()}, enforced entirely by {@link SecurityConfig}'s filter chain
 * (see that class' own Javadoc), so the real chain must actually run for a 401 test to mean
 * anything. {@link SecurityConfig} itself, plus its {@link CustomAuthenticationEntryPoint}/
 * {@link CustomAccessDeniedHandler} collaborators, are explicitly {@code @Import}ed since
 * {@code @WebMvcTest} does not component-scan plain {@code @Configuration}/{@code @Component}
 * classes on its own; {@code JwtAuthFilter} is the one exception, auto-included because it is
 * a servlet {@code Filter} bean. {@code JwtService} is mocked purely so {@code JwtAuthFilter}
 * can be constructed; unlike {@code auth-service}, there is no {@code UserDetailsService}/
 * {@code BlacklistedTokenRepository} to mock here, since {@code customer-service} owns no
 * user data.
 * <p>
 * Authenticated requests use {@code .with(user("test"))} (a
 * {@code SecurityMockMvcRequestPostProcessors} request post-processor) rather than the
 * class/method-level {@code @WithMockUser} annotation: with a real filter chain actually
 * running ({@code SecurityContextHolderFilter} reloads the context per request under
 * {@code SessionCreationPolicy.STATELESS}), {@code @WithMockUser}'s thread-local context is
 * not guaranteed to reach the filter chain, while the request post-processor sets it as part
 * of the request itself, which {@code SecurityContextHolderFilter} does pick up.
 * <p>
 * {@code CustomerExceptionHandler} maps {@code MethodArgumentNotValidException} to 400 and
 * {@code ResourceNotFoundException} (via {@code BaseExceptionHandler}) to 404; the tests
 * below assert those codes.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@AutoConfigureMockMvc
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private JwtService jwtService;

    private static CustomerResponse response(long id) {
        return new CustomerResponse(id, 1L, "Ada", "Lovelace", "+1234567890", "ada@example.com", "123 Main St");
    }

    @Test
    void findByIdReturns200WhenAuthenticated() throws Exception {
        when(customerService.findById(1L)).thenReturn(response(1L));

        mockMvc.perform(get("/api/v1/customers/1").with(user("test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("ada@example.com"));
    }

    @Test
    void findByIdReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/customers/1"))
                .andExpect(status().isUnauthorized());

        verify(customerService, never()).findById(any());
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(customerService.findById(99L)).thenThrow(new ResourceNotFoundException("No customer found with id 99"));

        mockMvc.perform(get("/api/v1/customers/99").with(user("test")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createReturns201WhenAuthenticated() throws Exception {
        CustomerRequest request = new CustomerRequest(1L, "Ada", "Lovelace", "+1234567890", "ada@example.com", "123 Main St");
        when(customerService.create(any())).thenReturn(response(1L));

        mockMvc.perform(post("/api/v1/customers")
                        .with(user("test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Ada"));
    }

    @Test
    void createReturnsUnauthorizedWithoutAuthentication() throws Exception {
        CustomerRequest request = new CustomerRequest(1L, "Ada", "Lovelace", "+1234567890", "ada@example.com", "123 Main St");

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(customerService, never()).create(any());
    }

    @Test
    void createRejectsInvalidPayload() throws Exception {
        CustomerRequest blank = new CustomerRequest(1L, " ", "Lovelace", "+1234567890", "ada@example.com", "123 Main St");

        mockMvc.perform(post("/api/v1/customers")
                        .with(user("test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blank)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(customerService, never()).create(any());
    }

    @Test
    void updateReturns200WhenAuthenticated() throws Exception {
        CustomerUpdateRequest request =
                new CustomerUpdateRequest("Grace", "Hopper", "+1987654321", "grace@example.com", "456 Oak Ave");
        when(customerService.update(any(), any())).thenReturn(response(1L));

        mockMvc.perform(put("/api/v1/customers/1")
                        .with(user("test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateReturnsUnauthorizedWithoutAuthentication() throws Exception {
        CustomerUpdateRequest request =
                new CustomerUpdateRequest("Grace", "Hopper", "+1987654321", "grace@example.com", "456 Oak Ave");

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(customerService, never()).update(any(), any());
    }
}
