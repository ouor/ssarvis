package com.ssarvis.backend.auth;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public AuthSession signUp(SignUpRequest request) {
        String username = normalize(request.username());
        String password = request.password();
        String displayName = normalize(request.displayName());

        if (userAccountRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken.");
        }

        UserAccount userAccount = userAccountRepository.save(new UserAccount(
                username,
                passwordEncoder.encode(password),
                displayName
        ));
        return toSession(userAccount);
    }

    public AuthSession login(LoginRequest request) {
        String username = normalize(request.username());
        UserAccount userAccount = userAccountRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));

        if (!passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }

        return toSession(userAccount);
    }

    public AuthenticatedUser getAuthenticatedUser(Long userId) {
        UserAccount userAccount = getActiveUserAccount(userId);

        return new AuthenticatedUser(
                userAccount.getId(),
                userAccount.getUsername(),
                userAccount.getDisplayName(),
                userAccount.getVisibility()
        );
    }

    public AuthenticatedUser touchActivityAndGetAuthenticatedUser(Long userId) {
        UserAccount userAccount = getActiveUserAccount(userId);
        userAccount.touchActivity();
        userAccountRepository.save(userAccount);
        return new AuthenticatedUser(
                userAccount.getId(),
                userAccount.getUsername(),
                userAccount.getDisplayName(),
                userAccount.getVisibility()
        );
    }

    public void updateVisibility(Long userId, AccountVisibility visibility) {
        UserAccount userAccount = getActiveUserAccount(userId);
        userAccount.updateVisibility(visibility);
        userAccountRepository.save(userAccount);
    }

    public AutoReplySettingsResponse getAutoReplySettings(Long userId) {
        UserAccount userAccount = getActiveUserAccount(userId);
        return new AutoReplySettingsResponse(userAccount.getAutoReplyMode(), userAccount.getLastActivityAt());
    }

    public AutoReplySettingsResponse updateAutoReplySettings(Long userId, AutoReplyMode autoReplyMode) {
        UserAccount userAccount = getActiveUserAccount(userId);
        userAccount.updateAutoReplyMode(autoReplyMode);
        userAccountRepository.save(userAccount);
        return new AutoReplySettingsResponse(userAccount.getAutoReplyMode(), userAccount.getLastActivityAt());
    }

    public void deactivate(Long userId) {
        UserAccount userAccount = getActiveUserAccount(userId);
        userAccount.softDelete();
        userAccountRepository.save(userAccount);
    }

    public UserAccount getActiveUserAccount(Long userId) {
        return userAccountRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found or inactive."));
    }

    public List<UserAccount> searchActiveUsers(Long currentUserId, String query, Pageable pageable) {
        getActiveUserAccount(currentUserId);
        return userAccountRepository.searchActiveUsers(currentUserId, normalize(query), pageable);
    }

    private AuthSession toSession(UserAccount userAccount) {
        return new AuthSession(
                userAccount.getId(),
                userAccount.getUsername(),
                userAccount.getDisplayName(),
                userAccount.getVisibility(),
                jwtTokenService.createAccessToken(userAccount)
        );
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
