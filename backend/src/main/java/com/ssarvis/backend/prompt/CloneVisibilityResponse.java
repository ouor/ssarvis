package com.ssarvis.backend.prompt;

public record CloneVisibilityResponse(
        Long cloneId,
        boolean isPublic
) {
}
