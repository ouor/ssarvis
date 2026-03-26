package com.ssarvis.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

public class JwtAuthenticationInterceptor implements HandlerInterceptor {
    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";

    private final JwtTokenService jwtTokenService;
    private final AuthService authService;

    public JwtAuthenticationInterceptor(JwtTokenService jwtTokenService, AuthService authService) {
        this.jwtTokenService = jwtTokenService;
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header is required.");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header is required.");
        }

        Long userId = jwtTokenService.parseUserId(token);
        request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, authService.getAuthenticatedUser(userId));
        return true;
    }
}
