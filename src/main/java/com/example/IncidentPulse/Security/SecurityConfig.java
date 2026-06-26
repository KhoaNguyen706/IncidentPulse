package com.example.IncidentPulse.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtFilter jwtFilter,
                                                   LoginRateLimitFilter loginRateLimitFilter,
                                                   ApiKeyFilter apiKeyFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                        .requestMatchers("/api/v*/incident/**").authenticated()
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
