package com.ssarvis.backend.voice;

import java.time.Instant;

public record VoiceSummaryResponse(
        Long registeredVoiceId,
        String voiceId,
        String preferredName,
        String originalFilename,
        String audioMimeType,
        Instant createdAt
) {
}
