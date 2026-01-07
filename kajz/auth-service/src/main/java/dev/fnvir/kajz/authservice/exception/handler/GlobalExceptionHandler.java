package dev.fnvir.kajz.authservice.exception.handler;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.fnvir.kajz.authservice.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var errors = ex.getBindingResult().getAllErrors().stream()
            .map(error -> Map.of(
                "field", error instanceof FieldError fe ? fe.getField() : error.getObjectName(),
                "message", error.getDefaultMessage()))
            .toList();

        var body = Map.of(
                "message", "Validation Failed",
                "timestamp", Instant.now().toString(),
                "path", req.getRequestURI(),
                "errors", errors
        );
        return ResponseEntity.badRequest().body(body);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest req) {
        var errors = e.getConstraintViolations().stream()
            .map(v -> {
                var path = v.getPropertyPath().toString();
                var field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                return Map.of("field", field, "message", v.getMessage());
            })
            .toList();
        var body = Map.of(
            "message", "Constraint Violation",
            "timestamp", Instant.now().toString(),
            "path", req.getRequestURI(),
            "errors", errors
        );
        return ResponseEntity.badRequest().body(body);
    }
    
    @ExceptionHandler({ ApiException.class })
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException e) {
        return ResponseEntity
                .status(e.getResponseStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(e.getResponseBody());
    }
    
}
