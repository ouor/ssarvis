package com.ssarvis.backend.voice;

public record VoiceRegisterResponse(
        Long registeredVoiceId,
        String voiceId,
        String displayName,
        String preferredName,
        String originalFilename,
        String audioMimeType
) {
}
