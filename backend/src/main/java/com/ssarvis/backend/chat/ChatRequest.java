package com.ssarvis.backend.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        Long promptGenerationLogId,
        Long conversationId,
        Long registeredVoiceId,
        @NotBlank(message = "message must not be blank.")
        String message
) {
}
