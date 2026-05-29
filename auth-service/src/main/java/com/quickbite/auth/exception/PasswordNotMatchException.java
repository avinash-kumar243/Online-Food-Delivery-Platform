package com.quickbite.auth.exception;

public class PasswordNotMatchException extends RuntimeException {
	public PasswordNotMatchException(String message) {
		super(message);
	}
}