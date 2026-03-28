package com.ssarvis.backend.dm;

import java.time.Instant;

public record DmThreadSummaryResponse(
        Long threadId,
        DmParticipantResponse otherParticipant,
        Instant createdAt,
        String latestMessagePreview,
        Instant latestMessageCreatedAt
) {
}
