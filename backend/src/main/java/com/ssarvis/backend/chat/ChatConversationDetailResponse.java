package com.ssarvis.backend.chat;

import java.time.Instant;
import java.util.List;

public record ChatConversationDetailResponse(
        Long conversationId,
        Long cloneId,
        String cloneAlias,
        String cloneShortDescription,
        Instant createdAt,
        List<ChatHistoryMessageResponse> messages
) {
}
