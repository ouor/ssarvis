package com.ssarvis.backend.auth;

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

        return new AuthenticatedUser(userAccount.getId(), userAccount.getUsername(), userAccount.getDisplayName());
    }

    public UserAccount getActiveUserAccount(Long userId) {
        return userAccountRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found or inactive."));
    }

    private AuthSession toSession(UserAccount userAccount) {
        return new AuthSession(
                userAccount.getId(),
                userAccount.getUsername(),
                userAccount.getDisplayName(),
                jwtTokenService.createAccessToken(userAccount)
        );
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
