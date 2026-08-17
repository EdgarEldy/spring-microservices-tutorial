package com.edgareldy.springmicroservicestutorial.orderservice.entity;

/**
 * The three states an {@link Order} moves through. {@code PENDING} is the only state
 * {@code OrderServiceImpl.create} ever sets directly; {@code CONFIRMED} and
 * {@code CONFIRMATION_FAILED} are only ever reached later, by
 * {@code OrderConfirmedEventListener}/{@code NotificationFailedEventListener} reacting to
 * the choreographed Saga's outcome events from {@code notification-service}. An order left
 * {@code PENDING} simply means the notification step has not resolved yet, never a stuck or
 * abandoned state by design (see README's Saga walkthrough).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CONFIRMATION_FAILED
}
