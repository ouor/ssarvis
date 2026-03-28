package com.ssarvis.backend.auth;

public record AuthSession(Long userId, String username, String displayName, AccountVisibility visibility, String accessToken) {
}
