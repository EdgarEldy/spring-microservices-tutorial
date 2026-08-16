package com.edgareldy.springmicroservicestutorial.notificationservice.service.impl;

import com.edgareldy.springmicroservicestutorial.notificationservice.service.EmailNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Default {@link EmailNotificationService} implementation: for this tutorial, "sending" an
 * e-mail means logging it, never a real delivery (SMTP, a transactional-email provider, ...).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Slf4j
@Service
public class EmailNotificationServiceImpl implements EmailNotificationService {

    @Override
    public void send(String recipientEmail, String subject, String body) {
        log.info("Sending e-mail to {} - subject: \"{}\" - body: \"{}\"", recipientEmail, subject, body);
    }
}
