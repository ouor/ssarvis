package com.ssarvis.backend.follow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "displayName must not be blank.")
        @Size(max = 100, message = "displayName must be at most 100 characters.")
        String displayName
) {
}
