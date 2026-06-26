package com.example.IncidentPulse.Security;

import com.example.IncidentPulse.ApplicationCofig.LoginRateLimitProperties;
import com.example.IncidentPulse.DTO.Response.ApiResponse;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection for the login endpoint. Keeps one in-memory
 * token bucket per client IP (sufficient for a single instance; swap for a
 * Redis-backed Bucket4j ProxyManager when running multiple replicas).
 *
 * Only POST requests to the auth login path are throttled; everything else
 * passes straight through (see {@link #shouldNotFilter}).
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String LOGIN_PATH = "/api/v*/auth/login";

    private final LoginRateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> bucketsByIp = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(LoginRateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && PATH_MATCHER.match(LOGIN_PATH, request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Bucket bucket = bucketsByIp.computeIfAbsent(clientIp(request), ip -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            writeTooManyRequests(response);
        }
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(
                properties.getCapacity(),
                Refill.intervally(properties.getRefillTokens(),
                        Duration.ofMinutes(properties.getRefillPeriodMinutes())));
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // First hop is the original client.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        ErrorCode errorCode = ErrorCode.RATE_LIMITED;
        ApiResponse<String> body = ApiResponse.<String>builder()
                .code(errorCode.getCode())
                .success(false)
                .data(errorCode.name())
                .now(LocalDateTime.now())
                .message(errorCode.getMessage())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
