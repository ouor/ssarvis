package com.ssarvis.backend.voice;

public record VoiceVisibilityResponse(
        Long registeredVoiceId,
        boolean isPublic
) {
}
