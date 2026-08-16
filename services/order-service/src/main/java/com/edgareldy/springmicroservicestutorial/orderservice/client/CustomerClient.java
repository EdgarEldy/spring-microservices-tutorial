package com.edgareldy.springmicroservicestutorial.orderservice.client;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.client.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign client for {@code customer-service}'s {@code GET /api/v1/customers/{id}},
 * resolved via Eureka ({@code name = "customer-service"} matches that service's own
 * {@code spring.application.name}) and load-balanced by Spring Cloud LoadBalancer. Used by
 * {@code OrderServiceImpl} to validate {@code customerId} synchronously before any local
 * write happens. Unlike {@link ProductClient}, {@code customer-service} requires
 * {@code authenticated()} on every {@code /api/v1/customers/**} route (customer data is PII,
 * see that service's own {@code SecurityConfig}), so this call genuinely depends on
 * {@code FeignConfig}'s {@code Authorization}-forwarding interceptor to succeed; without a
 * caller-supplied JWT, this call gets a 401 from {@code customer-service}, which {@code
 * OrderServiceImpl} maps the same way as any other {@code FeignException}.
 * <p>
 * {@code url = "${customer-service.url:}"} is left blank by default, so Eureka + Spring Cloud
 * LoadBalancer resolve {@code name = "customer-service"} normally in dev/prod. Tests override
 * {@code customer-service.url} to point this client straight at a WireMock server instead,
 * bypassing service discovery entirely for the "WireMock stubs for CustomerClient" test
 * requirement.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@FeignClient(name = "customer-service", url = "${customer-service.url:}")
public interface CustomerClient {

    @GetMapping("/api/v1/customers/{id}")
    ApiResponse<CustomerResponse> getCustomer(@PathVariable("id") Long id);
}
