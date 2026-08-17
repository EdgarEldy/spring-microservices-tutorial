package com.edgareldy.springmicroservicestutorial.notificationservice.event;

import com.edgareldy.springmicroservicestutorial.notificationservice.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link UserRegisteredEvent} off the {@code auth-events} topic and sends the account
 * activation "e-mail" via {@link EmailNotificationService}. Fire-and-forget: unlike the order
 * flow, there is no compensating event back to {@code auth-service} if this fails (a failed
 * activation e-mail does not need to undo the account creation, see that event's own Javadoc).
 * <p>
 * Declares its own {@code groupId}, distinct from {@link PasswordResetRequestedEventConsumer}'s:
 * {@code auth-events} carries both event types, and each consumer needs its own full copy of
 * the topic (a shared consumer group would split partitions between the two consumer classes,
 * so only one of them would ever see any given record).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventConsumer {

    private final EmailNotificationService emailNotificationService;

    @KafkaListener(topics = "auth-events", groupId = "notification-service-user-registered")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for userId={}", event.userId());
        emailNotificationService.send(
                event.email(),
                "Activate your account",
                "Hi " + event.firstName() + ", activate your account with this token: " + event.activationToken());
    }
}
