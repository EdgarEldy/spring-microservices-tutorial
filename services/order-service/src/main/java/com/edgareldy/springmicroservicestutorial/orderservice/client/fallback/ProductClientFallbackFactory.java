package com.edgareldy.springmicroservicestutorial.orderservice.client.fallback;

import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import com.edgareldy.springmicroservicestutorial.orderservice.client.ProductClient;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Resilience4j circuit breaker fallback for {@link ProductClient}, activated by
 * {@code spring.cloud.openfeign.circuitbreaker.enabled=true} and {@code @FeignClient(fallbackFactory = ...)}:
 * runs whenever the protected call fails, whether the circuit is open (short-circuited
 * immediately, no network call at all) or closed but the call itself threw (a timeout, a
 * connection refusal, a 5xx from {@code catalog-service}).
 * <p>
 * A plain {@code fallback} (no access to the causing exception) would route a legitimate 404
 * through the same generic path as a genuine outage, breaking the existing "no product with
 * this id" distinction. {@link FallbackFactory} is used instead specifically so {@link
 * #create} can inspect {@code cause} and preserve that distinction: {@link
 * FeignException.NotFound} rethrows {@link ResourceNotFoundException} (a real answer, "this
 * product does not exist", not a resilience concern, also configured via
 * {@code resilience4j.circuitbreaker.configs.default.ignore-exceptions} so a 404 never counts
 * toward the failure rate that opens the circuit in the first place: an {@code instances.
 * catalog-service.*} key would silently do nothing here, since Spring Cloud OpenFeign names
 * each breaker after the Feign method, not the {@code @FeignClient} name), anything else
 * throws {@link BusinessRuleException} with the message the README asks for. This
 * replaces the equivalent try/catch that used to live in {@code OrderServiceImpl.resolveProduct}
 * (see that class' git history): the fallback is now the single place this translation happens.
 * <p>
 * Created by Edgar Muhamyangabo on 8/17/26
 * Author : Edgar Muhamyangabo
 * Date : 8/17/26
 * Project : spring-microservices-tutorial
 */
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        return productId -> {
            if (cause instanceof FeignException.NotFound) {
                throw new ResourceNotFoundException("No product found with id " + productId);
            }
            throw new BusinessRuleException("Product service unavailable");
        };
    }
}
