package com.ssarvis.backend.follow;

import com.ssarvis.backend.auth.AccountVisibility;

public record UserProfileResponse(
        Long userId,
        String username,
        String displayName,
        AccountVisibility visibility,
        boolean me,
        boolean following
) {
}
