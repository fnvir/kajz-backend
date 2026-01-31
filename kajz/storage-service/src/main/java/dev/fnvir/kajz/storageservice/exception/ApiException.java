package dev.fnvir.kajz.storageservice.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;
import lombok.Setter;

/** Generic Exception for REST API response. */
@Getter
@Setter
public class ApiException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HttpStatusCode responseStatus;

    public ApiException() {
        this(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ApiException(int statusCode) {
        this(HttpStatusCode.valueOf(statusCode));
    }

    public ApiException(HttpStatusCode status) {
        this(status, status.toString());
    }

    public ApiException(int statusCode, String message) {
        this(HttpStatusCode.valueOf(statusCode), message);
    }

    public ApiException(HttpStatusCode status, String message) {
        super(message);
        this.responseStatus = status;
    }

    public ApiException(HttpStatusCode status, String message, Throwable cause) {
        super(message, cause);
        this.responseStatus = status;
    }

    public ApiException(HttpStatusCode status, Throwable cause) {
        super(cause);
        this.responseStatus = status;
    }

    public ApiException(HttpStatusCode status, String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.responseStatus = status;
    }

}
