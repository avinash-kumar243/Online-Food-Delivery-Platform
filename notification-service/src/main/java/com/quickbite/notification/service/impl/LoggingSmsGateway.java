package com.quickbite.notification.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.quickbite.notification.entity.Notification;
import com.quickbite.notification.service.SmsGateway;

@Component
public class LoggingSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsGateway.class);

    @Override
    public void send(Notification notification) {
        log.info("SMS placeholder dispatch. recipientId={}, title={}, providerHint=Twilio/AWS SNS",
                notification.getRecipientId(), notification.getTitle());
    }
}
