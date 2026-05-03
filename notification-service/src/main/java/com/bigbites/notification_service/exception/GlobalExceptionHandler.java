package com.bigbites.notification_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Generic response builder
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);

        return new ResponseEntity<>(body, status);
    }

    // ── 404 NOT FOUND ─────────────────────────────
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationNotFound(NotificationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", ex.getMessage());
    }

    // ── FEIGN ERRORS (VERY IMPORTANT IN YOUR CASE) ──
    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeign(feign.FeignException ex) {
        return build(
                HttpStatus.SERVICE_UNAVAILABLE,
                "DOWNSTREAM_SERVICE_ERROR",
                "One of the microservices is unavailable: " + ex.getMessage()
        );
    }

    // ── NULL POINTER / BAD DATA ─────────────────────
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNull(NullPointerException ex) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "NULL_POINTER",
                "Unexpected null value occurred"
        );
    }

    // ── GENERIC FALLBACK ───────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                ex.getMessage()
        );
    }
}