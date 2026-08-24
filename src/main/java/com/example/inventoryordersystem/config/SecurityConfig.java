package com.example.inventoryordersystem.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;

    public SecurityConfig(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                // CSRF無効化（REST API / JWT認証のため）
                .csrf(AbstractHttpConfigurer::disable)
                // セッションはSTATELESS（保持しない）に設定
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 未認証(401)および権限不足(403)のハンドリング設定
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden")
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        // 公開エンドポイント
                        .requestMatchers("/api/v1/health", "/api/v1/auth/**").permitAll()
                        // それ以外のAPI（/api/v1/health/protected を含む）は認証必須
                        .anyRequest().authenticated()
                )
                // UsernamePasswordAuthenticationFilter の前に JWT フィルターを適用
                .addFilterBefore(
                        new JwtAuthenticationFilter(tokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

}
