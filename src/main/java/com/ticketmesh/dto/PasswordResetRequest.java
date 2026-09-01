package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "Reset token is required") String token,
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
        String newPassword
) {}
