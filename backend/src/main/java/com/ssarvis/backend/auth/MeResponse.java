package com.ssarvis.backend.auth;

public record MeResponse(Long userId, String username, String displayName) {
}
