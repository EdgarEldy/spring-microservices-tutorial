package com.edgareldy.springmicroservicestutorial.orderservice.client.fallback;

import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import com.edgareldy.springmicroservicestutorial.orderservice.client.CustomerClient;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Resilience4j circuit breaker fallback for {@link CustomerClient}. Same rationale as {@link
 * ProductClientFallbackFactory}: a {@link FallbackFactory} rather than a plain {@code
 * fallback} so {@link #create} can distinguish a genuine 404 ({@link
 * FeignException.NotFound}, rethrown as {@link ResourceNotFoundException}, also excluded from
 * the circuit's own failure count via {@code
 * resilience4j.circuitbreaker.configs.default.ignore-exceptions}, not an {@code instances.
 * customer-service.*} key, since Spring Cloud OpenFeign names each breaker after the Feign
 * method rather than the {@code @FeignClient} name) from an actual outage ({@link
 * BusinessRuleException}).
 * <p>
 * Created by Edgar Muhamyangabo on 8/17/26
 * Author : Edgar Muhamyangabo
 * Date : 8/17/26
 * Project : spring-microservices-tutorial
 */
@Component
public class CustomerClientFallbackFactory implements FallbackFactory<CustomerClient> {

    @Override
    public CustomerClient create(Throwable cause) {
        return customerId -> {
            if (cause instanceof FeignException.NotFound) {
                throw new ResourceNotFoundException("No customer found with id " + customerId);
            }
            throw new BusinessRuleException("Customer service unavailable");
        };
    }
}
