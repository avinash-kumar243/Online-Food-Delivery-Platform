package com.quickbite.auth.exception;

import org.springframework.http.HttpStatus;

import com.quickbite.auth.enums.UserStatus;

public class AccountAccessException extends RuntimeException {

    private final UserStatus status;
    private final HttpStatus httpStatus;

    public AccountAccessException(UserStatus status, String message) {
        super(message);
        this.status = status;
        this.httpStatus = HttpStatus.FORBIDDEN;
    }

    public UserStatus getStatus() {
        return status;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
