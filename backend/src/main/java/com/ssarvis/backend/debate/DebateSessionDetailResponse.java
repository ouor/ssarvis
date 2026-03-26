package com.ssarvis.backend.debate;

import java.time.Instant;
import java.util.List;

public record DebateSessionDetailResponse(
        Long debateSessionId,
        Long cloneAId,
        String cloneAAlias,
        String cloneAShortDescription,
        Long cloneAVoiceId,
        Long cloneBId,
        String cloneBAlias,
        String cloneBShortDescription,
        Long cloneBVoiceId,
        String topic,
        Instant createdAt,
        List<DebateHistoryTurnResponse> turns
) {
}
