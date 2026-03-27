package com.ssarvis.backend.friend;

import java.time.Instant;

public record FriendSummaryResponse(
        FriendUserSummaryResponse user,
        Instant friendsSince
) {
}
