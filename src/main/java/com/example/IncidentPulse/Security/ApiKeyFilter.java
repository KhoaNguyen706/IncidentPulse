package com.example.IncidentPulse.Security;

import com.example.IncidentPulse.DTO.Response.ApiResponse;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

/**
 * Authenticates webhook requests by a shared secret in the {@code X-API-Key}
 * header. Scoped to the webhook path only; all other requests pass through to
 * the JWT chain. Fails closed: if no key is configured, every webhook request
 * is rejected.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String WEBHOOK_PATH = "/api/v*/webhook/**";
    private static final String API_KEY_HEADER = "X-API-Key";

    private final String configuredApiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(@Value("${app.webhook.api-key:}") String configuredApiKey, ObjectMapper objectMapper) {
        this.configuredApiKey = configuredApiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Admin simulate endpoint uses JWT instead of X-API-Key.
        if (uri.contains("/webhook/simulate")) {
            return true;
        }
        return !PATH_MATCHER.match(WEBHOOK_PATH, uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String provided = request.getHeader(API_KEY_HEADER);
        if (!isValid(provided)) {
            writeUnauthorized(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isValid(String provided) {
        if (configuredApiKey == null || configuredApiKey.isBlank() || provided == null) {
            return false;
        }
        // Constant-time comparison to avoid leaking the key via timing.
        return MessageDigest.isEqual(
                configuredApiKey.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        ErrorCode errorCode = ErrorCode.INVALID_API_KEY;
        ApiResponse<String> body = ApiResponse.<String>builder()
                .code(errorCode.getCode())
                .success(false)
                .data(errorCode.name())
                .now(LocalDateTime.now())
                .message(errorCode.getMessage())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
