package com.ssarvis.backend.debate;

import java.time.Instant;

public record DebateSessionSummaryRow(
        Long debateSessionId,
        Long cloneAId,
        String cloneAAlias,
        Long cloneBId,
        String cloneBAlias,
        String topic,
        Instant createdAt,
        long turnCount
) {
}
