package com.edgareldy.springmicroservicestutorial.authservice.dto.auth;

/**
 * Response returned by {@code POST /api/v1/auth/login}, carrying the signed JWT the
 * client must present as a bearer token on subsequent requests.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record AuthResponse(String token) {
}
