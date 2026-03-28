package com.ssarvis.backend.dm;

import com.ssarvis.backend.auth.AccountVisibility;

public record DmParticipantResponse(
        Long userId,
        String username,
        String displayName,
        AccountVisibility visibility
) {
}
