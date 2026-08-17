package com.edgareldy.springmicroservicestutorial.notificationservice.event;

import com.edgareldy.springmicroservicestutorial.notificationservice.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link PasswordResetRequestedEvent} off the {@code auth-events} topic and sends the
 * password-reset "e-mail" via {@link EmailNotificationService}. Fire-and-forget, same rationale
 * as {@link UserRegisteredEventConsumer}: a failed reset e-mail does not need to undo the token
 * already issued.
 * <p>
 * See {@link UserRegisteredEventConsumer}'s own Javadoc for why this consumer declares its own
 * distinct {@code groupId}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetRequestedEventConsumer {

    private final EmailNotificationService emailNotificationService;

    @KafkaListener(topics = "auth-events", groupId = "notification-service-password-reset-requested")
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Received PasswordResetRequestedEvent for userId={}", event.userId());
        emailNotificationService.send(
                event.email(),
                "Reset your password",
                "Reset your password with this token: " + event.resetToken());
    }
}
