package com.edgareldy.springmicroservicestutorial.orderservice.client.fallback;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import com.edgareldy.springmicroservicestutorial.orderservice.client.ProductClient;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link ProductClientFallbackFactory}'s {@link
 * FeignException.NotFound}-vs-anything-else translation (see feature/resilience): the
 * responsibility {@link com.edgareldy.springmicroservicestutorial.orderservice.service.impl.OrderServiceImpl}
 * used to own directly before this branch moved it here (see that class' own test class,
 * {@code OrderServiceImplTest}, whose equivalent cases now assert the raw {@link
 * FeignException} propagates unchanged instead). No Spring context, no circuit breaker, no
 * Feign client involved: {@link #create} is called directly with a hand-built cause.
 * <p>
 * Created by Edgar Muhamyangabo on 8/17/26
 * Author : Edgar Muhamyangabo
 * Date : 8/17/26
 * Project : spring-microservices-tutorial
 */
class ProductClientFallbackFactoryTest {

    private final ProductClientFallbackFactory factory = new ProductClientFallbackFactory();

    @Test
    void create_notFoundCause_returnsClientThatThrowsResourceNotFoundException() {
        ProductClient fallback = factory.create(notFound());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> fallback.getProduct(1L))
                .withMessageContaining("1");
    }

    @Test
    void create_anyOtherCause_returnsClientThatThrowsBusinessRuleException() {
        ProductClient fallback = factory.create(serviceUnavailable());

        assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> fallback.getProduct(1L));
    }

    @Test
    void create_nonFeignCause_stillReturnsClientThatThrowsBusinessRuleException() {
        // The circuit being OPEN (no HTTP call attempted at all) surfaces as a
        // resilience4j-internal exception, not a FeignException; this must still be treated
        // as "service unavailable", never mistaken for a 404.
        ProductClient fallback = factory.create(new RuntimeException("circuit open"));

        assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> fallback.getProduct(1L));
    }

    private static FeignException.NotFound notFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.NotFound("not found", request, null, null);
    }

    private static FeignException.ServiceUnavailable serviceUnavailable() {
        Request request = Request.create(Request.HttpMethod.GET, "/", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.ServiceUnavailable("unavailable", request, null, null);
    }
}
