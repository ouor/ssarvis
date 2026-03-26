package com.ssarvis.backend.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnBean({JwtTokenService.class, AuthService.class})
    JwtAuthenticationInterceptor jwtAuthenticationInterceptor(
            JwtTokenService jwtTokenService,
            AuthService authService
    ) {
        return new JwtAuthenticationInterceptor(jwtTokenService, authService);
    }
}
