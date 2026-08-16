package com.edgareldy.springmicroservicestutorial.orderservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import com.edgareldy.springmicroservicestutorial.orderservice.config.SecurityConfig;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderDetailResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderRequest;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.entity.OrderStatus;
import com.edgareldy.springmicroservicestutorial.orderservice.security.CustomAccessDeniedHandler;
import com.edgareldy.springmicroservicestutorial.orderservice.security.CustomAuthenticationEntryPoint;
import com.edgareldy.springmicroservicestutorial.orderservice.security.JwtService;
import com.edgareldy.springmicroservicestutorial.orderservice.service.OrderService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc slice tests for {@link OrderController}, with {@link OrderService} mocked. Same
 * pattern as {@code customer-service}'s {@code CustomerControllerTest}: servlet filters left
 * enabled (no {@code addFilters = false}) since there is no {@code @PreAuthorize}/{@code
 * MethodSecurityConfig} in this service, {@link SecurityConfig} plus its {@link
 * CustomAuthenticationEntryPoint}/{@link CustomAccessDeniedHandler} collaborators explicitly
 * {@code @Import}ed, {@code JwtService} mocked purely so {@code JwtAuthFilter} can be
 * constructed. Authenticated requests use
 * {@code .with(SecurityMockMvcRequestPostProcessors.user("test"))} rather than
 * {@code @WithMockUser}, for the same reason documented in
 * {@code CustomerControllerTest}'s own Javadoc: a real filter chain running under
 * {@code SessionCreationPolicy.STATELESS} does not reliably pick up {@code @WithMockUser}'s
 * thread-local context.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@AutoConfigureMockMvc
class OrderControllerTest {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    private static OrderResponse response(long id) {
        return new OrderResponse(id, 1L, 1L, 2, 79.80, OrderStatus.PENDING);
    }

    private static OrderDetailResponse detailResponse(long id) {
        return new OrderDetailResponse(id, 1L, 1L, 2, 79.80, OrderStatus.PENDING, "Clean Code", "Ada Lovelace");
    }

    @Test
    void findByIdReturns200WhenAuthenticated() throws Exception {
        when(orderService.findById(1L)).thenReturn(detailResponse(1L));

        mockMvc.perform(get("/api/v1/orders/1").with(user("test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productName").value("Clean Code"))
                .andExpect(jsonPath("$.data.customerFullName").value("Ada Lovelace"));
    }

    @Test
    void findByIdReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isUnauthorized());

        verify(orderService, never()).findById(any());
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(orderService.findById(99L)).thenThrow(new ResourceNotFoundException("No order found with id 99"));

        mockMvc.perform(get("/api/v1/orders/99").with(user("test")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createReturns201WhenAuthenticatedWithIdempotencyKeyHeader() throws Exception {
        OrderRequest request = new OrderRequest(1L, 1L, 2);
        when(orderService.create(any(), org.mockito.ArgumentMatchers.eq("key-1"))).thenReturn(response(1L));

        mockMvc.perform(post("/api/v1/orders")
                        .with(user("test"))
                        .header(IDEMPOTENCY_KEY_HEADER, "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(79.80));
    }

    @Test
    void createReturnsBadRequestWithoutIdempotencyKeyHeader() throws Exception {
        OrderRequest request = new OrderRequest(1L, 1L, 2);

        mockMvc.perform(post("/api/v1/orders")
                        .with(user("test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).create(any(), any());
    }

    @Test
    void createReturnsUnauthorizedWithoutAuthentication() throws Exception {
        OrderRequest request = new OrderRequest(1L, 1L, 2);

        mockMvc.perform(post("/api/v1/orders")
                        .header(IDEMPOTENCY_KEY_HEADER, "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(orderService, never()).create(any(), any());
    }

    @Test
    void createRejectsInvalidPayload() throws Exception {
        OrderRequest invalid = new OrderRequest(null, 1L, 2);

        mockMvc.perform(post("/api/v1/orders")
                        .with(user("test"))
                        .header(IDEMPOTENCY_KEY_HEADER, "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(orderService, never()).create(any(), any());
    }

    @Test
    void findAllReturns200WhenAuthenticated() throws Exception {
        when(orderService.findAll(any())).thenReturn(new PageImpl<>(List.of(response(1L)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/orders").with(user("test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void findAllReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());

        verify(orderService, never()).findAll(any());
    }
}
