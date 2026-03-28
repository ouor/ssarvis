package com.ssarvis.backend.dm;

import java.time.Instant;

public record DmMessageResponse(
        Long messageId,
        Long senderUserId,
        String senderDisplayName,
        boolean aiGenerated,
        Long bundleRootMessageId,
        String content,
        Instant createdAt
) {
}
