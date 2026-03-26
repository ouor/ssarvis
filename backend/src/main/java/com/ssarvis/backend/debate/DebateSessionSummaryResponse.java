package com.ssarvis.backend.debate;

import java.time.Instant;

public record DebateSessionSummaryResponse(
        Long debateSessionId,
        Long cloneAId,
        String cloneAAlias,
        Long cloneBId,
        String cloneBAlias,
        String topic,
        Instant createdAt,
        int turnCount
) {
}
