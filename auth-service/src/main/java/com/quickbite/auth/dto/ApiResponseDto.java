package com.quickbite.auth.dto;

public record ApiResponseDto(
    boolean success,
    String message
) {
}
