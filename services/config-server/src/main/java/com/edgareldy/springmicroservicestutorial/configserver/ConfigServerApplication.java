package com.edgareldy.springmicroservicestutorial.configserver;

import com.edgareldy.springmicroservicestutorial.commonlib.logging.LoggingAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.Import;

/**
 * Bootstraps the Spring Cloud Config Server that centralizes each business
 * service's application configuration, served from a native/classpath-backed
 * config repo (no Git backend) under {@code config-repo/}.
 * <p>
 * {@code @Import(LoggingAspect.class)} (see feature/observability): bare
 * {@code @SpringBootApplication} only component-scans this class' own package and
 * its sub-packages, never {@code common-lib}'s sibling package, so
 * {@code LoggingAspect} (a plain {@code @Component @Aspect}) is never auto-detected
 * without this. See {@code OrderServiceApplication}'s own Javadoc for why this is a
 * single-class {@code @Import} rather than a wider {@code @ComponentScan}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@SpringBootApplication
@Import(LoggingAspect.class)
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
