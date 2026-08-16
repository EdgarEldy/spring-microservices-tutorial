package com.edgareldy.springmicroservicestutorial.customerservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PUT /api/v1/customers/{id}}. Deliberately has no
 * {@code userId} field, unlike {@link CustomerRequest}: once a customer
 * profile is created, the {@code userId} it was created for never changes
 * over its lifetime, the same way a real identity binding wouldn't be
 * silently reassigned to a different user via a plain profile edit. The
 * service layer must leave {@code Customer.userId} untouched on update,
 * regardless of what a caller might otherwise attempt to send.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record CustomerUpdateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String telephone,
        @NotBlank @Email String email,
        @NotBlank String address
) {
}
