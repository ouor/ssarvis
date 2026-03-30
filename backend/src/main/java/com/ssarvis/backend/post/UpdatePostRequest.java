package com.ssarvis.backend.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @NotBlank(message = "content must not be blank.")
        @Size(max = 2000, message = "content must be at most 2000 characters.")
        String content
) {
}
