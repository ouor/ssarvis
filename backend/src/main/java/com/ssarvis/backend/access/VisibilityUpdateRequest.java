package com.ssarvis.backend.access;

import jakarta.validation.constraints.NotNull;

public record VisibilityUpdateRequest(
        @NotNull(message = "isPublic is required.")
        Boolean isPublic
) {
}
