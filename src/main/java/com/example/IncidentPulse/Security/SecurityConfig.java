package com.example.IncidentPulse.Security;

import com.example.IncidentPulse.DTO.Response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtFilter jwtFilter,
                                                   LoginRateLimitFilter loginRateLimitFilter,
                                                   ApiKeyFilter apiKeyFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(),
                                    ApiResponse.<String>builder()
                                            .code(401)
                                            .success(false)
                                            .data("UNAUTHORIZED")
                                            .now(LocalDateTime.now())
                                            .message("Authentication required")
                                            .build());
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(),
                                    ApiResponse.<String>builder()
                                            .code(403)
                                            .success(false)
                                            .data("FORBIDDEN")
                                            .now(LocalDateTime.now())
                                            .message("You do not have permission to perform this action")
                                            .build());
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v*/auth/**").permitAll()
                        // JWT-protected demo endpoint; ApiKeyFilter skips /simulate.
                        .requestMatchers("/api/v*/webhook/simulate").authenticated()
                        // Public webhook alert; ApiKeyFilter enforces the X-API-Key.
                        .requestMatchers("/api/v*/webhook/**").permitAll()
                        // WebSocket handshake is permitted; the STOMP CONNECT frame
                        // is authenticated by StompAuthChannelInterceptor instead.
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        .requestMatchers("/api/v*/users/me").authenticated()
                        .requestMatchers("/api/v*/incident", "/api/v*/incident/**").authenticated()
                        .requestMatchers("/api/v*/on-call", "/api/v*/on-call/**").authenticated()
                        .requestMatchers("/api/v*/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Rate limiter runs first so brute-forced logins are rejected
                // before any heavier work; the API-key filter guards the webhook
                // path; then the JWT filter authenticates everything else.
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
