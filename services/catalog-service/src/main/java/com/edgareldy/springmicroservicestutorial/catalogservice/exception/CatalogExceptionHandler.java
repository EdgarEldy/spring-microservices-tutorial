package com.edgareldy.springmicroservicestutorial.catalogservice.exception;

import com.edgareldy.springmicroservicestutorial.commonlib.exception.BaseExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Activates {@link BaseExceptionHandler}'s {@code ResourceNotFoundException}/
 * {@code BusinessRuleException}/catch-all mappings for {@code catalog-service}.
 * Left deliberately empty: this service has no exception case beyond what the
 * base class already covers (unlike, say, {@code auth-service}'s own
 * {@code AuthExceptionHandler}, which layers on Spring Security-specific
 * cases). Spring only wires up {@code @ExceptionHandler} methods declared on
 * a bean itself annotated {@code @RestControllerAdvice}, so this subclass
 * still has to exist even with an empty body.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@RestControllerAdvice
public class CatalogExceptionHandler extends BaseExceptionHandler {
}
