package dev.fnvir.kajz.storageservice.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConflictException() {
        super(HttpStatus.CONFLICT, "Conflict");
    }

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public ConflictException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, message, cause);
    }

    public ConflictException(Throwable cause) {
        super(HttpStatus.CONFLICT, cause);
    }
    
}
