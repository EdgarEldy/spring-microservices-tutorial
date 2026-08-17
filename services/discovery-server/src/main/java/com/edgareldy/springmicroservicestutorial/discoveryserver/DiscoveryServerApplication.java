package com.edgareldy.springmicroservicestutorial.discoveryserver;

import com.edgareldy.springmicroservicestutorial.commonlib.logging.LoggingAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Import;

/**
 * Bootstraps the standalone Eureka service registry that every other service in the
 * system registers with and discovers instances through; runs as a server only, it
 * never registers itself or fetches a registry from another Eureka instance.
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
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
