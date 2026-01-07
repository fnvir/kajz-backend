package dev.fnvir.kajz.authservice.exception;

import java.io.Serial;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;
import lombok.Setter;

/** Generic Exception for REST API response */
@Getter
@Setter
public class ApiException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HttpStatusCode responseStatus;
    private final Map<String, Object> responseBody;

    public ApiException() {
        this(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ApiException(int statusCode) {
        this(HttpStatusCode.valueOf(statusCode));
    }

    public ApiException(HttpStatusCode status) {
        this(status, "Error");
    }

    public ApiException(int statusCode, String message) {
        this(HttpStatusCode.valueOf(statusCode), message);
    }

    public ApiException(int statusCode, Map<String, Object> responseBody) {
        this(HttpStatusCode.valueOf(statusCode), responseBody);
    }

    public ApiException(HttpStatusCode status, Map<String, Object> responseBody) {
        super(String.valueOf(responseBody.getOrDefault("message", "Error")));
        this.responseStatus = status;
        this.responseBody = Map.copyOf(responseBody);
    }

    public ApiException(HttpStatusCode status, String message) {
        super(message);
        this.responseStatus = status;
        this.responseBody = Map.of("message", message);
    }

    public ApiException(HttpStatusCode status, String message, Throwable cause) {
        super(message, cause);
        this.responseStatus = status;
        this.responseBody = Map.of("message", message);
    }

    public ApiException(HttpStatusCode status, Throwable cause) {
        super(cause);
        this.responseStatus = status;
        this.responseBody = Map.of("message", super.getMessage());
    }

    public ApiException(HttpStatusCode status, String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.responseStatus = status;
        this.responseBody = Map.of("message", super.getMessage());
    }

}
