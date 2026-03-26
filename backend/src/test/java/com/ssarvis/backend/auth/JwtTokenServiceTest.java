package com.ssarvis.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ssarvis.backend.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class JwtTokenServiceTest {

    private AppProperties appProperties;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getAuth().getJwt().setSecret("test-jwt-secret-key-that-is-at-least-32-bytes-long");
        appProperties.getAuth().getJwt().setAccessTokenExpirationMinutes(120);
        jwtTokenService = new JwtTokenService(appProperties);
    }

    @Test
    void createAndParseAccessTokenReturnsUserId() {
        UserAccount userAccount = assignId(new UserAccount("haru", "hashed", "하루"), 7L);

        String token = jwtTokenService.createAccessToken(userAccount);
        Long userId = jwtTokenService.parseUserId(token);

        assertThat(token).isNotBlank();
        assertThat(userId).isEqualTo(7L);
    }

    @Test
    void parseUserIdRejectsInvalidToken() {
        assertThatThrownBy(() -> jwtTokenService.parseUserId("invalid-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getReason()).isEqualTo("Invalid or expired access token.");
                });
    }

    @Test
    void createAccessTokenRejectsShortSecret() {
        appProperties.getAuth().getJwt().setSecret("too-short-secret");

        assertThatThrownBy(() -> jwtTokenService.createAccessToken(assignId(new UserAccount("haru", "hashed", "하루"), 1L)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(exception.getReason()).isEqualTo("APP_AUTH_JWT_SECRET must be at least 32 bytes long.");
                });
    }

    private UserAccount assignId(UserAccount userAccount, Long id) {
        try {
            java.lang.reflect.Field idField = UserAccount.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(userAccount, id);
            return userAccount;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign test id.", exception);
        }
    }
}
