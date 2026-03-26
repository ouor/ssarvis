package com.ssarvis.backend.config;

import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final long STREAM_TIMEOUT_MILLIS = 5 * 60 * 1000;

    private final AppProperties appProperties;
    private final ObjectProvider<JwtAuthenticationInterceptor> jwtAuthenticationInterceptorProvider;

    public WebConfig(
            AppProperties appProperties,
            ObjectProvider<JwtAuthenticationInterceptor> jwtAuthenticationInterceptorProvider
    ) {
        this.appProperties = appProperties;
        this.jwtAuthenticationInterceptorProvider = jwtAuthenticationInterceptorProvider;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(appProperties.getCors().getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS");
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(STREAM_TIMEOUT_MILLIS);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        JwtAuthenticationInterceptor jwtAuthenticationInterceptor = jwtAuthenticationInterceptorProvider.getIfAvailable();
        if (jwtAuthenticationInterceptor != null) {
            registry.addInterceptor(jwtAuthenticationInterceptor)
                    .addPathPatterns(
                            "/api/auth/me",
                            "/api/clones",
                            "/api/clones/**",
                            "/api/voices",
                            "/api/voices/**",
                            "/api/chat/**",
                            "/api/debates/**",
                            "/api/system-prompt"
                    );
        }
    }
}
