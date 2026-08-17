package com.edgareldy.springmicroservicestutorial.orderservice;

import com.edgareldy.springmicroservicestutorial.commonlib.logging.LoggingAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
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
 * {@code @Import(LoggingAspect.class)}: bare {@code @SpringBootApplication} only
 * component-scans this class' own package and its sub-packages
 * ({@code .orderservice.**}), never a sibling one. {@code common-lib}'s
 * {@code LoggingAspect} lives under {@code com.edgareldy.springmicroservicestutorial.commonlib},
 * a sibling, not a sub-package, of {@code .orderservice}; without this explicit
 * import, {@code LoggingAspect} (a plain {@code @Component @Aspect}, meant to be
 * auto-detected) is never registered as a bean at all and its {@code @Around}
 * advice never runs, silently, with no error anywhere. A first attempt at this
 * fix used a wider {@code @ComponentScan(basePackages =
 * "com.edgareldy.springmicroservicestutorial")} instead; that also fixed
 * {@code LoggingAspect}, but broke {@code OrderControllerTest} ({@code
 * @WebMvcTest}'s slice-scoped {@code TypeExcludeFilter} no longer reliably kept
 * {@code JwtService}'s real bean out of the context, so its {@code @MockitoBean}
 * override stopped taking effect and the real constructor ran against a
 * too-short test JWT secret). A single-class {@code @Import} sidesteps that
 * entirely: it registers exactly one additional bean, with no interaction with
 * component-scan-based slice test filtering at all.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@SpringBootApplication
@Import(LoggingAspect.class)
@EnableFeignClients
@EnableKafka
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
