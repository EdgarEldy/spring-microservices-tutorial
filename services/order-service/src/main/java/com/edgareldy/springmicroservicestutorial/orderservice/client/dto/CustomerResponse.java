package com.edgareldy.springmicroservicestutorial.orderservice.client.dto;

/**
 * Local mirror of {@code customer-service}'s own {@code CustomerResponse}, deserialized from
 * {@code GET /api/v1/customers/{id}} via {@link
 * com.edgareldy.springmicroservicestutorial.orderservice.client.CustomerClient}. Never the
 * same Java class as {@code customer-service}'s: services never share domain-specific DTOs
 * (see {@code .claude/CLAUDE.md}'s {@code common-lib} rule), each side of a Feign call keeps
 * its own local shape for the same wire contract.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public record CustomerResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String telephone,
        String email,
        String address
) {
}
