package com.example.IncidentPulse.Exception;

import com.example.IncidentPulse.DTO.Response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Centralised error handling. Every exception thrown by a controller or
 * service ends up here so the client always sees a uniform JSON shape:
 *
 *   { code, success, data, now, message }
 *
 * Keep stack traces out of the response body in production - log them
 * server-side instead.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<String>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = httpStatusFor(errorCode.getCode());
        return ResponseEntity.status(status).body(
                ApiResponse.<String>builder()
                        .code(errorCode.getCode())
                        .success(false)
                        .data(errorCode.name())
                        .now(LocalDateTime.now())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                        .code(400)
                        .success(false)
                        .data("VALIDATION_FAILED")
                        .now(LocalDateTime.now())
                        .message(details.isEmpty() ? "Validation failed" : details)
                        .build());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<String>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.<String>builder()
                        .code(401)
                        .success(false)
                        .data("UNAUTHORIZED")
                        .now(LocalDateTime.now())
                        .message("Authentication required")
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.<String>builder()
                        .code(403)
                        .success(false)
                        .data("FORBIDDEN")
                        .now(LocalDateTime.now())
                        .message("You do not have permission to perform this action")
                        .build());
    }

    /**
     * Catch-all so we never leak a Spring stack trace to the client.
     * Logs the real cause on the server.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<String>builder()
                        .code(500)
                        .success(false)
                        .data("INTERNAL_SERVER_ERROR")
                        .now(LocalDateTime.now())
                        .message("Something went wrong. Please try again later.")
                        .build());
    }

    private HttpStatus httpStatusFor(int code) {
        HttpStatus resolved = HttpStatus.resolve(code);
        return resolved != null ? resolved : HttpStatus.BAD_REQUEST;
    }
}
