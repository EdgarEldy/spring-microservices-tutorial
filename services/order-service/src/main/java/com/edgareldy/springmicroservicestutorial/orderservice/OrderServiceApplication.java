package com.edgareldy.springmicroservicestutorial.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Bootstraps the orders service: synchronous product/customer validation via
 * OpenFeign, idempotent order creation, and the choreographed Saga's
 * OrderCreatedEvent trigger. {@code @EnableFeignClients} activates
 * {@code ProductClient}/{@code CustomerClient}, without it neither interface
 * would ever get a Spring-managed proxy implementation. {@code @EnableKafka}
 * is explicit here even though Spring Boot's own {@code KafkaAutoConfiguration}
 * already carries it, so the listener wiring this service depends on
 * ({@code OrderConfirmedEventListener}/{@code NotificationFailedEventListener})
 * is not left implicit.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@SpringBootApplication
@EnableFeignClients
@EnableKafka
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
