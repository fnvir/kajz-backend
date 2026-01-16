package dev.fnvir.kajz.notificationservice.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN, "Forbidden");
    }

    public ForbiddenException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, message, cause);
    }

    public ForbiddenException(Throwable cause) {
        super(HttpStatus.CONFLICT, cause);
    }
    
}
