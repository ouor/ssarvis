package com.ssarvis.backend.friend;

public record UserSearchResponse(
        Long userId,
        String username,
        String displayName
) {
}
