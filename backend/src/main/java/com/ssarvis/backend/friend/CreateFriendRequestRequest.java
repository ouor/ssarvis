package com.ssarvis.backend.friend;

import jakarta.validation.constraints.NotNull;

public record CreateFriendRequestRequest(
        @NotNull(message = "receiverUserId is required.")
        Long receiverUserId
) {
}
