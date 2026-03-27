package com.ssarvis.backend.friend;

public record FriendUserSummaryResponse(
        Long userId,
        String username,
        String displayName
) {
}
