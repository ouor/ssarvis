package com.ssarvis.backend.dm;

import java.time.Instant;

public record DmMessageResponse(
        Long messageId,
        Long senderUserId,
        String senderDisplayName,
        String content,
        Instant createdAt
) {
}
