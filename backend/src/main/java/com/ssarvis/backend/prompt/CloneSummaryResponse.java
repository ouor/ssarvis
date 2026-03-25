package com.ssarvis.backend.prompt;

import java.time.Instant;

public record CloneSummaryResponse(
        Long cloneId,
        Instant createdAt,
        String alias,
        String shortDescription
) {
}
