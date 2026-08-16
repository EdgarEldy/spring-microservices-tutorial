package com.edgareldy.springmicroservicestutorial.orderservice.client;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign client for {@code catalog-service}'s {@code GET /api/v1/catalog/products/{id}},
 * resolved via Eureka ({@code name = "catalog-service"} matches that service's
 * {@code spring.application.name}, the same value used for its own Eureka registration) and
 * load-balanced by Spring Cloud LoadBalancer. Used by {@code OrderServiceImpl} to validate
 * {@code productId} and price an order synchronously, before any local write happens. That
 * endpoint is public (no JWT required, see {@code catalog-service}'s own
 * {@code SecurityConfig}), unlike {@link CustomerClient}, so no {@code Authorization} header
 * needs to reach this particular call, even though {@code FeignConfig}'s interceptor forwards
 * one to every outgoing Feign request regardless.
 * <p>
 * {@code url = "${catalog-service.url:}"} is left blank by default, so Eureka + Spring Cloud
 * LoadBalancer resolve {@code name = "catalog-service"} normally in dev/prod. Tests override
 * {@code catalog-service.url} to point this client straight at a WireMock server instead,
 * bypassing service discovery entirely for the "WireMock stubs for ProductClient" test
 * requirement.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@FeignClient(name = "catalog-service", url = "${catalog-service.url:}")
public interface ProductClient {

    @GetMapping("/api/v1/catalog/products/{id}")
    ApiResponse<ProductResponse> getProduct(@PathVariable("id") Long id);
}
