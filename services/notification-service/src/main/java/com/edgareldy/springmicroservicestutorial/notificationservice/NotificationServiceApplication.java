package com.edgareldy.springmicroservicestutorial.notificationservice;

import com.edgareldy.springmicroservicestutorial.commonlib.logging.LoggingAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
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
 * {@code @Import(LoggingAspect.class)} (see feature/observability): bare
 * {@code @SpringBootApplication} only component-scans this class' own package and
 * its sub-packages, never {@code common-lib}'s sibling package, so
 * {@code LoggingAspect} (a plain {@code @Component @Aspect}) is never auto-detected
 * without this. See {@code order-service}'s {@code OrderServiceApplication} own
 * Javadoc for why this is a single-class {@code @Import} rather than a wider
 * {@code @ComponentScan}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@SpringBootApplication
@Import(LoggingAspect.class)
@EnableKafka
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
