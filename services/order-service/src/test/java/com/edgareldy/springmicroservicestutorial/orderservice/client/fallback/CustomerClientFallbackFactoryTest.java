package com.edgareldy.springmicroservicestutorial.orderservice.client.fallback;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import com.edgareldy.springmicroservicestutorial.orderservice.client.CustomerClient;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link CustomerClientFallbackFactory}'s {@link
 * FeignException.NotFound}-vs-anything-else translation. Same rationale and pattern as {@link
 * ProductClientFallbackFactoryTest}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/17/26
 * Author : Edgar Muhamyangabo
 * Date : 8/17/26
 * Project : spring-microservices-tutorial
 */
class CustomerClientFallbackFactoryTest {

    private final CustomerClientFallbackFactory factory = new CustomerClientFallbackFactory();

    @Test
    void create_notFoundCause_returnsClientThatThrowsResourceNotFoundException() {
        CustomerClient fallback = factory.create(notFound());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> fallback.getCustomer(1L))
                .withMessageContaining("1");
    }

    @Test
    void create_anyOtherCause_returnsClientThatThrowsBusinessRuleException() {
        CustomerClient fallback = factory.create(unauthorized());

        assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> fallback.getCustomer(1L));
    }

    private static FeignException.NotFound notFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.NotFound("not found", request, null, null);
    }

    private static FeignException.Unauthorized unauthorized() {
        Request request = Request.create(Request.HttpMethod.GET, "/", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.Unauthorized("unauthorized", request, null, null);
    }
}
