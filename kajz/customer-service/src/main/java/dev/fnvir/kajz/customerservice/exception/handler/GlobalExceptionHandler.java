package dev.fnvir.kajz.customerservice.exception.handler;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.fnvir.kajz.customerservice.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var errors = ex.getBindingResult().getAllErrors().stream()
            .map(error -> Map.of(
                "field", error instanceof FieldError fe ? fe.getField() : error.getObjectName(),
                "message", error.getDefaultMessage()))
            .toList();

        var body = ErrorResponse.builder()
                .message("Validation Failed")
                .path(req.getRequestURI())
                .errors(errors)
                .build();
        
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(body);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest req) {
        var errors = e.getConstraintViolations().stream()
            .map(v -> {
                var path = v.getPropertyPath().toString();
                var field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                return Map.of("field", field, "message", v.getMessage());
            })
            .toList();
        
        var body = ErrorResponse.builder()
                .message("Constraint Violation")
                .path(req.getRequestURI())
                .errors(errors)
                .build();
        
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(body);
    }
    
    @ExceptionHandler({ ApiException.class })
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e, HttpServletRequest req) {
        return ResponseEntity
                .status(e.getResponseStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(
                        e.getMessage(),
                        req.getRequestURI()
                ));
    }
    
    @Builder
    public record ErrorResponse (
        String message,
        String path,
        Instant timestamp,
        List<Map<String, String>> errors
    ) {
        public ErrorResponse {
            timestamp = timestamp != null ? timestamp : Instant.now();
            errors = errors != null ? errors : List.of();
        }
        public ErrorResponse(String message, String path) {
            this(message, path, null, null);
        }
    }
    
}
