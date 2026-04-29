package com.quickbite.notification.service;

import com.quickbite.notification.entity.Notification;

public interface SmsGateway {

    void send(Notification notification);
}
