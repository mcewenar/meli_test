package com.hackerrank.sample.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private static final List<String> PUBLIC_PATHS = List.of(
            "/",
            "/actuator/health",
            "/actuator/info",
            "/actuator/metrics",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**"
    );

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${app.security.api-key:}") String apiKey,
            @Value("${app.security.api-key-header:X-API-Key}") String headerName
    ) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        if (apiKey == null || apiKey.isBlank()) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS.toArray(new String[0])).permitAll()
                .anyRequest().authenticated()
        );
        http.addFilterBefore(
                new ApiKeyAuthFilter(apiKey, headerName),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}
