package dev.fnvir.kajz.authservice.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NotFoundException() {
        super(HttpStatus.NOT_FOUND, "Not found");
    }

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, message, cause);
    }

    public NotFoundException(Throwable cause) {
        super(HttpStatus.NOT_FOUND, cause);
    }
    
}