package com.ssarvis.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userAccountRepository, passwordEncoder, jwtTokenService);
    }

    @Test
    void signUpCreatesUserWithHashedPassword() {
        given(userAccountRepository.existsByUsername("haru")).willReturn(false);
        given(userAccountRepository.save(any())).willAnswer(invocation -> {
            UserAccount saved = invocation.getArgument(0);
            return reflectId(saved, 1L);
        });
        given(jwtTokenService.createAccessToken(any())).willReturn("signed-token");

        AuthSession session = authService.signUp(new SignUpRequest(" haru ", "secret123", " 하루 "));

        assertThat(session.userId()).isEqualTo(1L);
        assertThat(session.username()).isEqualTo("haru");
        assertThat(session.displayName()).isEqualTo("하루");
        assertThat(session.accessToken()).isEqualTo("signed-token");

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("haru");
        assertThat(captor.getValue().getDisplayName()).isEqualTo("하루");
        assertThat(passwordEncoder.matches("secret123", captor.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void signUpRejectsDuplicateUsername() {
        given(userAccountRepository.existsByUsername("haru")).willReturn(true);

        assertThatThrownBy(() -> authService.signUp(new SignUpRequest("haru", "secret123", "하루")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Username is already taken.");
                });
    }

    @Test
    void loginReturnsSessionForActiveUser() {
        UserAccount userAccount = reflectId(new UserAccount("miso", passwordEncoder.encode("pass1234"), "미소"), 2L);
        given(userAccountRepository.findByUsernameAndDeletedAtIsNull("miso")).willReturn(Optional.of(userAccount));
        given(jwtTokenService.createAccessToken(userAccount)).willReturn("login-token");

        AuthSession session = authService.login(new LoginRequest(" miso ", "pass1234"));

        assertThat(session.userId()).isEqualTo(2L);
        assertThat(session.username()).isEqualTo("miso");
        assertThat(session.displayName()).isEqualTo("미소");
        assertThat(session.accessToken()).isEqualTo("login-token");
    }

    @Test
    void loginRejectsWrongPassword() {
        UserAccount userAccount = reflectId(new UserAccount("miso", passwordEncoder.encode("pass1234"), "미소"), 2L);
        given(userAccountRepository.findByUsernameAndDeletedAtIsNull("miso")).willReturn(Optional.of(userAccount));

        assertThatThrownBy(() -> authService.login(new LoginRequest("miso", "wrong-pass")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getReason()).isEqualTo("Invalid username or password.");
                });
    }

    @Test
    void getAuthenticatedUserReturnsActiveUser() {
        UserAccount userAccount = reflectId(new UserAccount("dami", passwordEncoder.encode("pass1234"), "다미"), 3L);
        given(userAccountRepository.findByIdAndDeletedAtIsNull(3L)).willReturn(Optional.of(userAccount));

        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(3L);

        assertThat(authenticatedUser.userId()).isEqualTo(3L);
        assertThat(authenticatedUser.username()).isEqualTo("dami");
        assertThat(authenticatedUser.displayName()).isEqualTo("다미");
    }

    @Test
    void touchActivityAndGetAuthenticatedUserUpdatesLastActivity() {
        UserAccount userAccount = reflectId(new UserAccount("dami", passwordEncoder.encode("pass1234"), "다미"), 3L);
        given(userAccountRepository.findByIdAndDeletedAtIsNull(3L)).willReturn(Optional.of(userAccount));
        given(userAccountRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        AuthenticatedUser authenticatedUser = authService.touchActivityAndGetAuthenticatedUser(3L);

        assertThat(authenticatedUser.userId()).isEqualTo(3L);
        assertThat(userAccount.getLastActivityAt()).isNotNull();
        assertThat(userAccount.getLastActivityAt()).isAfterOrEqualTo(before);
        verify(userAccountRepository).save(userAccount);
    }

    @Test
    void updateAutoReplySettingsPersistsMode() {
        UserAccount userAccount = reflectId(new UserAccount("nara", passwordEncoder.encode("pass1234"), "나라"), 4L);
        given(userAccountRepository.findByIdAndDeletedAtIsNull(4L)).willReturn(Optional.of(userAccount));
        given(userAccountRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AutoReplySettingsResponse response = authService.updateAutoReplySettings(4L, AutoReplyMode.AWAY);

        assertThat(response.mode()).isEqualTo(AutoReplyMode.AWAY);
        assertThat(userAccount.getAutoReplyMode()).isEqualTo(AutoReplyMode.AWAY);
        verify(userAccountRepository).save(userAccount);
    }

    @Test
    void getAuthenticatedUserRejectsSoftDeletedUser() {
        given(userAccountRepository.findByIdAndDeletedAtIsNull(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getAuthenticatedUser(9L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getReason()).isEqualTo("User not found or inactive.");
                });
    }

    @Test
    void deactivateSoftDeletesActiveUser() {
        UserAccount userAccount = reflectId(new UserAccount("nara", passwordEncoder.encode("pass1234"), "나라"), 4L);
        given(userAccountRepository.findByIdAndDeletedAtIsNull(4L)).willReturn(Optional.of(userAccount));

        authService.deactivate(4L);

        assertThat(userAccount.isDeleted()).isTrue();
        verify(userAccountRepository).save(userAccount);
    }

    @Test
    void deactivateRejectsInactiveUser() {
        given(userAccountRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.deactivate(10L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getReason()).isEqualTo("User not found or inactive.");
                });

        verify(userAccountRepository, never()).save(any());
    }

    private UserAccount reflectId(UserAccount userAccount, Long id) {
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
