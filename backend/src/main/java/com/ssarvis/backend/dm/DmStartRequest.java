package com.ssarvis.backend.dm;

import jakarta.validation.constraints.NotNull;

public record DmStartRequest(
        @NotNull(message = "targetUserId is required.")
        Long targetUserId
) {
}
