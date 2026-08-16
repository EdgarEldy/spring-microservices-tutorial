package com.edgareldy.springmicroservicestutorial.notificationservice.service;

/**
 * The single place through which every outbound e-mail leaves the system, regardless of which
 * business event triggered it: order confirmations, account activation, and password reset all
 * go through this one method. Deliberately generic (recipient/subject/body) rather than one
 * method per e-mail kind, so this stays genuinely "one place that sends e-mail" instead of
 * three specialized methods dressed up as shared. Each of the three Kafka consumers is
 * responsible for building its own subject/body from its own event's data before calling this.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public interface EmailNotificationService {

    /**
     * "Sends" an e-mail. For this tutorial, the implementation only logs the message instead
     * of delivering a real e-mail (see README's Scope).
     */
    void send(String recipientEmail, String subject, String body);
}
