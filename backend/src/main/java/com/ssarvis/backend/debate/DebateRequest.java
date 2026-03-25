package com.ssarvis.backend.debate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DebateRequest(
        Long cloneAId,
        Long cloneBId,
        Long cloneAVoiceId,
        Long cloneBVoiceId,
        @NotBlank(message = "topic must not be blank.")
        String topic,
        @Min(value = 1, message = "turnsPerClone must be at least 1.")
        @Max(value = 3, message = "turnsPerClone must be at most 3.")
        int turnsPerClone
) {
}
