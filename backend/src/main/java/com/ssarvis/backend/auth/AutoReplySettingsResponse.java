package com.ssarvis.backend.auth;

import java.time.Instant;

public record AutoReplySettingsResponse(
        AutoReplyMode mode,
        Instant lastActivityAt
) {
}
