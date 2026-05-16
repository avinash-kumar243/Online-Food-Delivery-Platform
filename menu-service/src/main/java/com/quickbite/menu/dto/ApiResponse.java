package com.quickbite.menu.dto;

import java.time.Instant;

public record ApiResponse(
    Instant timestamp,
    String message
) {
    public static ApiResponse of(String message) {
        return new ApiResponse(Instant.now(), message);
    }
}
