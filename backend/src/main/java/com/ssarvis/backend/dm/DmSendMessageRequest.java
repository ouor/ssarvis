package com.ssarvis.backend.dm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DmSendMessageRequest(
        @NotBlank(message = "content must not be blank.")
        @Size(max = 4000, message = "content must be at most 4000 characters.")
        String content
) {
}
