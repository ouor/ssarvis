package com.ssarvis.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank(message = "username must not be blank.")
        @Size(max = 50, message = "username must be at most 50 characters.")
        String username,

        @NotBlank(message = "password must not be blank.")
        @Size(min = 4, max = 100, message = "password must be between 4 and 100 characters.")
        String password,

        @NotBlank(message = "displayName must not be blank.")
        @Size(max = 100, message = "displayName must be at most 100 characters.")
        String displayName
) {
}
