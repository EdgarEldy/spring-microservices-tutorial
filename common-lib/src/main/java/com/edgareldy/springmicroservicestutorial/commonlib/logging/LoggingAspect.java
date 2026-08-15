package com.edgareldy.springmicroservicestutorial.commonlib.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Cross-cutting AOP advice that logs entry and exit of every service-layer
 * method across all modules, so call flow can be traced without each
 * service hand-rolling its own logging. It relies entirely on whatever
 * trace context (traceId/spanId) Micrometer Tracing places in the SLF4J MDC
 * once {@code feature/observability} is merged; until then it simply logs
 * without one, since propagating trace context is not this aspect's job.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Matches every method of every class in a {@code service} (sub-)package
     * of any module under the project's root package, e.g.
     * {@code com.edgareldy.springmicroservicestutorial.catalogservice.service.impl.ProductServiceImpl}.
     */
    @Around("execution(* com.edgareldy.springmicroservicestutorial..service..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        log.debug("Entering {} with arguments {}", signature, Arrays.toString(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            log.debug("Exiting {} with result {}", signature, result);
            return result;
        } catch (Throwable ex) {
            log.info("Exiting {} with exception {}", signature, ex.toString());
            throw ex;
        }
    }
}
