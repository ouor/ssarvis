package com.ssarvis.backend.chat;

public record ChatResponse(
        Long conversationId,
        String assistantMessage
) {
}
