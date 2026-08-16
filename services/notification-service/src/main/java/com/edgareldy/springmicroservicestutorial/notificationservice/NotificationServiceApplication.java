package com.edgareldy.springmicroservicestutorial.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Bootstraps the notification service: the system's sole database-less,
 * consumer-only microservice, responsible for every outbound e-mail and for
 * the choreographed Saga's confirming/compensating events back to
 * order-service. {@code @EnableKafka} is explicit even though Spring Boot's
 * own {@code KafkaAutoConfiguration} already carries it, so the three
 * {@code @KafkaListener} consumers this service is built entirely around are
 * not left implicit.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@SpringBootApplication
@EnableKafka
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
