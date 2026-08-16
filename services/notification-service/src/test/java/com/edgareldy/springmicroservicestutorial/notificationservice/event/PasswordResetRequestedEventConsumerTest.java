package com.edgareldy.springmicroservicestutorial.notificationservice.event;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.edgareldy.springmicroservicestutorial.notificationservice.service.EmailNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure Mockito unit test for {@link PasswordResetRequestedEventConsumer}: {@link
 * EmailNotificationService} is mocked, no Spring context, no real Kafka broker.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetRequestedEventConsumerTest {

    @Mock
    private EmailNotificationService emailNotificationService;

    @Test
    void onPasswordResetRequested_sendsResetEmailToTheEventsAddress() {
        PasswordResetRequestedEventConsumer consumer = new PasswordResetRequestedEventConsumer(emailNotificationService);
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(1L, "ada@example.com", "reset-token");

        consumer.onPasswordResetRequested(event);

        verify(emailNotificationService).send(eq("ada@example.com"), anyString(), anyString());
    }
}
