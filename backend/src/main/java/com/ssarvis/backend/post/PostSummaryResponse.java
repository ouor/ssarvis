package com.ssarvis.backend.post;

import com.ssarvis.backend.auth.AccountVisibility;
import java.time.Instant;

public record PostSummaryResponse(
        Long postId,
        Long ownerUserId,
        String ownerUsername,
        String ownerDisplayName,
        AccountVisibility ownerVisibility,
        String content,
        Instant createdAt
) {
}
