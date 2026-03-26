package com.ssarvis.backend.debate;

import java.time.Instant;

public record DebateHistoryTurnResponse(
        int turnIndex,
        String speaker,
        Long cloneId,
        String content,
        Instant createdAt,
        String ttsAudioUrl,
        String ttsVoiceId
) {
}
