package com.edgareldy.springmicroservicestutorial.catalogservice;

import com.edgareldy.springmicroservicestutorial.commonlib.logging.LoggingAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Bootstraps the catalog service: categories and products, including the
 * product detail endpoint order-service resolves via OpenFeign.
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
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
