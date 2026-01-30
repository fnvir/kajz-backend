package dev.fnvir.kajz.storageservice.exception.handler;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import dev.fnvir.kajz.storageservice.dto.res.ErrorResponse;
import dev.fnvir.kajz.storageservice.exception.ApiException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidationExceptions(WebExchangeBindException ex, ServerHttpRequest req) {
        var errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> Map.of(
                    "field", error instanceof FieldError fe ? fe.getField() : error.getObjectName(),
                    "message", error.getDefaultMessage()))
                .toList();

        var body = Map.of(
                "message", "Validation Failed",
                "timestamp", Instant.now().toString(),
                "path", req.getURI().getPath(),
                "errors", errors
        );
        return Mono.just(ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(body));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, ServerHttpRequest req) {
        var errors = ex.getBindingResult().getAllErrors().stream()
            .map(error -> Map.of(
                "field", error instanceof FieldError fe ? fe.getField() : error.getObjectName(),
                "message", error.getDefaultMessage()))
            .toList();

        var body = Map.of(
                "message", "Validation Failed",
                "timestamp", Instant.now().toString(),
                "path", req.getURI().getPath(),
                "errors", errors
        );
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(body);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException e, ServerHttpRequest req) {
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
            "path", req.getURI().getPath(),
            "errors", errors
        );
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(body);
    }
    
    @ExceptionHandler({ ApiException.class })
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e, ServerHttpRequest req) {
        return ResponseEntity
                .status(e.getResponseStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(
                        e.getResponseStatus().value(),
                        e.getMessage(),
                        req.getPath().value()
                ));
    }
    
}
