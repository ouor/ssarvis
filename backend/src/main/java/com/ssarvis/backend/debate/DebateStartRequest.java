package com.ssarvis.backend.debate;

import jakarta.validation.constraints.NotBlank;

public record DebateStartRequest(
        Long cloneAId,
        Long cloneBId,
        Long cloneAVoiceId,
        Long cloneBVoiceId,
        @NotBlank(message = "topic must not be blank.")
        String topic
) {
}
