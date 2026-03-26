package com.ssarvis.backend.auth;

public record AuthenticatedUser(Long userId, String username, String displayName) {
}
