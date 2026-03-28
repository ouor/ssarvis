package com.ssarvis.backend.dm;

import java.time.Instant;
import java.util.List;

public record DmThreadDetailResponse(
        Long threadId,
        DmParticipantResponse otherParticipant,
        Instant createdAt,
        List<DmMessageResponse> messages
) {
}
