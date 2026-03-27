package com.ssarvis.backend.friend;

import java.time.Instant;

public record FriendRequestResponse(
        Long friendRequestId,
        FriendRequestStatus status,
        Instant createdAt,
        Instant respondedAt,
        FriendUserSummaryResponse requester,
        FriendUserSummaryResponse receiver
) {
}
