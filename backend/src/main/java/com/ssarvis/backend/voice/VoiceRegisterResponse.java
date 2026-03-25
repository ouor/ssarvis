package com.ssarvis.backend.voice;

public record VoiceRegisterResponse(
        Long registeredVoiceId,
        String voiceId,
        String preferredName,
        String originalFilename,
        String audioMimeType
) {
}
