package com.ssarvis.backend.chat;

import java.time.Instant;

public record ChatConversationSummaryRow(
        Long conversationId,
        Long cloneId,
        String cloneAlias,
        Instant createdAt,
        String latestMessageContent,
        long messageCount
) {
}
