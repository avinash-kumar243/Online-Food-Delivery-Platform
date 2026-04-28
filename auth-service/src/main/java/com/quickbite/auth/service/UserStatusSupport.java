package com.quickbite.auth.service;

import org.springframework.stereotype.Component;

import com.quickbite.auth.enums.UserStatus;

@Component
public class UserStatusSupport {

    public UserStatus resolve(UserStatus status, Boolean isActive) {
        if (status != null) {
            return status;
        }
        return Boolean.FALSE.equals(isActive) ? UserStatus.SUSPENDED : UserStatus.ACTIVE;
    }

    public void ensureActive(UserStatus status, String subject) {
        if (status == UserStatus.SUSPENDED) {
            throw new RuntimeException(subject + " is suspended");
        }
        if (status == UserStatus.DELETED) {
            throw new RuntimeException(subject + " is deleted");
        }
    }
}
