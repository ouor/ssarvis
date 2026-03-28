package com.ssarvis.backend.follow;

import com.ssarvis.backend.auth.AccountVisibility;
import jakarta.validation.constraints.NotNull;

public record VisibilityUpdateRequest(
        @NotNull(message = "visibility is required.")
        AccountVisibility visibility
) {
}
