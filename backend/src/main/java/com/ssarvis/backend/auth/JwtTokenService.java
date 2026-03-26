package com.ssarvis.backend.auth;

import com.ssarvis.backend.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JwtTokenService {

    private final AppProperties appProperties;

    public JwtTokenService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String createAccessToken(UserAccount userAccount) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(appProperties.getAuth().getJwt().getAccessTokenExpirationMinutes() * 60);

        return Jwts.builder()
                .subject(String.valueOf(userAccount.getId()))
                .claim("username", userAccount.getUsername())
                .claim("displayName", userAccount.getDisplayName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token.");
        }
    }

    private SecretKey getSigningKey() {
        String secret = appProperties.getAuth().getJwt().getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "APP_AUTH_JWT_SECRET is not configured.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "APP_AUTH_JWT_SECRET must be at least 32 bytes long."
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
