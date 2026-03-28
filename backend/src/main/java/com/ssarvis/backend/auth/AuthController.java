package com.ssarvis.backend.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public AuthResponse signUp(@Valid @RequestBody SignUpRequest request) {
        AuthSession session = authService.signUp(request);
        return new AuthResponse(session.userId(), session.username(), session.displayName(), session.visibility(), session.accessToken());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AuthSession session = authService.login(request);
        return new AuthResponse(session.userId(), session.username(), session.displayName(), session.visibility(), session.accessToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        // JWT access tokens are stateless. The client should discard the token on logout.
    }

    @GetMapping("/me")
    public MeResponse me(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return new MeResponse(user.userId(), user.username(), user.displayName(), user.visibility());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        authService.deactivate(user.userId());
    }
}
