package com.ssarvis.backend.debate;

import java.util.List;

public record DebateResponse(
        Long debateSessionId,
        String topic,
        List<DebateTurnResponse> turns
) {
}
