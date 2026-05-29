package com.quickbite.auth.service;

import org.springframework.stereotype.Component;

import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.exception.AccountAccessException;

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
            throw new AccountAccessException(UserStatus.SUSPENDED, subject + " is suspended");
        }
        if (status == UserStatus.DELETED) {
            throw new AccountAccessException(UserStatus.DELETED, subject + " is deleted");
        }
    }
}
