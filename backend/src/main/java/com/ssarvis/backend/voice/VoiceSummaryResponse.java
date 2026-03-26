package com.ssarvis.backend.voice;

import java.time.Instant;

public record VoiceSummaryResponse(
        Long registeredVoiceId,
        String voiceId,
        String displayName,
        String preferredName,
        String originalFilename,
        String audioMimeType,
        Instant createdAt,
        boolean isPublic,
        String ownerDisplayName
) {
}
