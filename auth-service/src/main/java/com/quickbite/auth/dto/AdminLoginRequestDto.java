package com.quickbite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequestDto(
    @NotBlank @Email String email,
    @NotBlank String password
) {
}
