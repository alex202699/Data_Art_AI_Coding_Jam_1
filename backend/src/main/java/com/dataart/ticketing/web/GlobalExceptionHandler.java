package com.dataart.ticketing.web;

import java.util.Map;

import com.dataart.ticketing.auth.AuthExceptions.EmailAlreadyRegisteredException;
import com.dataart.ticketing.auth.AuthExceptions.EmailNotVerifiedException;
import com.dataart.ticketing.auth.AuthExceptions.InvalidCredentialsException;
import com.dataart.ticketing.auth.AuthExceptions.InvalidTokenException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Maps exceptions to the {@code { "error": "..." }} response contract. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(EmailAlreadyRegisteredException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(InvalidCredentialsException e) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(EmailNotVerifiedException e) {
        Map<String, Object> body = Map.of("error", e.getMessage(), "code", "email_not_verified");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler({InvalidTokenException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e) {
        String message = (e instanceof MethodArgumentNotValidException)
                ? "One or more fields are invalid."
                : e.getMessage();
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(DataIntegrityViolationException e) {
        // Backstop for referential-integrity / unique violations that slip past the
        // pre-checks (e.g. a concurrent delete). The specific services normally map
        // these to precise messages first.
        return error(HttpStatus.CONFLICT, "The operation conflicts with existing data.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException e) {
        String reason = e.getReason() != null ? e.getReason() : e.getStatusCode().toString();
        return error(HttpStatus.valueOf(e.getStatusCode().value()), reason);
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
