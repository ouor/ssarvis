package com.ssarvis.backend.auth;

public record AuthSession(Long userId, String username, String displayName, String accessToken) {
}
