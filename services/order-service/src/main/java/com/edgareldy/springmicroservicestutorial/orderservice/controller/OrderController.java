package com.edgareldy.springmicroservicestutorial.orderservice.controller;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.commonlib.dto.PageResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderDetailResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderRequest;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes {@code /api/v1/orders}: idempotent creation, a paginated listing, and a detail
 * endpoint enriched with product/customer data resolved via Feign. Every route requires an
 * authenticated caller, enforced by {@code SecurityConfig}'s {@code authenticated()} rule, so
 * no {@code @PreAuthorize} is needed on any handler here (same posture as
 * {@code customer-service}, and for a second, load-bearing reason: {@code FeignConfig}
 * forwards the caller's JWT on to {@code CustomerClient}, which itself requires one).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order creation and lookup")
public class OrderController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final OrderService orderService;

    @Operation(
            summary = "Get an order by id",
            description = "Enriched with product/customer data resolved via Feign (API Composition)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.findById(id), "Order retrieved"));
    }

    @Operation(
            summary = "Create an order",
            description = "Requires an Idempotency-Key header; validates customerId/productId synchronously via "
                    + "Feign; publishes OrderCreatedEvent asynchronously after commit")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody OrderRequest request,
            @Parameter(description = "Client-supplied idempotency key, retried requests reuse the same order")
                    @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey) {
        OrderResponse response = orderService.create(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Order created"));
    }

    @Operation(summary = "List orders", description = "Paginated list of every order")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> findAll(Pageable pageable) {
        Page<OrderResponse> page = orderService.findAll(pageable);
        PageResponse<OrderResponse> response = PageResponse.of(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(response, "Orders retrieved"));
    }
}
