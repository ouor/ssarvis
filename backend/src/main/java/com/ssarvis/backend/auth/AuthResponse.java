package com.ssarvis.backend.auth;

public record AuthResponse(Long userId, String username, String displayName, String accessToken) {
}
