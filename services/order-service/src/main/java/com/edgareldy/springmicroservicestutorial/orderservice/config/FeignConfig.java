package com.edgareldy.springmicroservicestutorial.orderservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Forwards the inbound request's {@code Authorization} header onto every outgoing Feign call
 * ({@link com.edgareldy.springmicroservicestutorial.orderservice.client.ProductClient}/
 * {@link com.edgareldy.springmicroservicestutorial.orderservice.client.CustomerClient}).
 * {@code customer-service} requires a valid JWT on every {@code /api/v1/customers/**} route
 * (see {@code CustomerClient}'s own Javadoc); without this, {@code CustomerClient} would get a
 * 401 from every call, regardless of whether the caller of {@code order-service} was
 * genuinely authenticated. This is why every {@code /api/v1/orders/**} route also requires
 * {@code authenticated()} in this service's own {@code SecurityConfig}: there would be no
 * caller JWT to forward otherwise.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Configuration
public class FeignConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Bean
    public RequestInterceptor authorizationForwardingInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            String authorizationHeader = attributes.getRequest().getHeader(AUTHORIZATION_HEADER);
            if (authorizationHeader != null) {
                requestTemplate.header(AUTHORIZATION_HEADER, authorizationHeader);
            }
        };
    }
}
