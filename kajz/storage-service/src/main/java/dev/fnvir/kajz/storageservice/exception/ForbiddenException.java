package dev.fnvir.kajz.storageservice.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN, "Forbidden");
    }

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(HttpStatus.FORBIDDEN, message, cause);
    }

    public ForbiddenException(Throwable cause) {
        super(HttpStatus.FORBIDDEN, cause);
    }
    
}
