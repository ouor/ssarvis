package com.ssarvis.backend.follow;

import com.ssarvis.backend.auth.AccountVisibility;

public record FollowUserSummaryResponse(
        Long userId,
        String username,
        String displayName,
        AccountVisibility visibility,
        boolean following
) {
}
