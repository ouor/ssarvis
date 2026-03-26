package com.ssarvis.backend.chat;

import java.time.Instant;

public record ChatHistoryMessageResponse(
        String role,
        String content,
        Instant createdAt,
        String ttsAudioUrl,
        String ttsVoiceId
) {
}
