package com.ssarvis.backend.chat;

import java.time.Instant;

public record ChatConversationSummaryResponse(
        Long conversationId,
        Long cloneId,
        String cloneAlias,
        Instant createdAt,
        String latestMessagePreview,
        int messageCount
) {
}
