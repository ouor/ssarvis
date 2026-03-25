package com.ssarvis.backend.debate;

public record DebateProgressResponse(
        Long debateSessionId,
        String topic,
        DebateTurnResponse turn
) {
}
