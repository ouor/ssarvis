package com.ssarvis.backend.access;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum AssetListScope {
    MINE,
    PUBLIC;

    public static AssetListScope from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return MINE;
        }

        return switch (rawValue.trim().toLowerCase()) {
            case "mine" -> MINE;
            case "public" -> PUBLIC;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scope must be one of: mine, public.");
        };
    }
}
