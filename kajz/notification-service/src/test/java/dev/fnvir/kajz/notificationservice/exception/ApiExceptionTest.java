package dev.fnvir.kajz.notificationservice.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("Exception Classes Unit Tests")
class ApiExceptionTest {

    @Nested
    @DisplayName("ApiException Tests")
    class ApiExceptionTests {

        @Test
        @DisplayName("Should create ApiException with default status")
        void shouldCreateApiExceptionWithDefaultStatus() {
            ApiException exception = new ApiException();

            assertEquals(exception.getResponseStatus(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should create ApiException with int status code")
        void shouldCreateApiExceptionWithIntStatusCode() {
            ApiException exception = new ApiException(400);

            Assertions.assertThat(exception.getResponseStatus().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("Should create ApiException with HttpStatus")
        void shouldCreateApiExceptionWithHttpStatus() {
            ApiException exception = new ApiException(HttpStatus.BAD_REQUEST);

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should create ApiException with status and message")
        void shouldCreateApiExceptionWithStatusAndMessage() {
            ApiException exception = new ApiException(HttpStatus.NOT_FOUND, "Resource not found");

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            Assertions.assertThat(exception.getMessage()).isEqualTo("Resource not found");
        }

        @Test
        @DisplayName("Should create ApiException with int status and message")
        void shouldCreateApiExceptionWithIntStatusAndMessage() {
            ApiException exception = new ApiException(404, "Not found");

            Assertions.assertThat(exception.getResponseStatus().value()).isEqualTo(404);
            Assertions.assertThat(exception.getMessage()).isEqualTo("Not found");
        }

        @Test
        @DisplayName("Should create ApiException with status, message and cause")
        void shouldCreateApiExceptionWithStatusMessageAndCause() {
            Throwable cause = new RuntimeException("Original error");

            ApiException exception = new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error", cause);

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            Assertions.assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("Should create ApiException with status and cause")
        void shouldCreateApiExceptionWithStatusAndCause() {
            Throwable cause = new RuntimeException("Original error");

            ApiException exception = new ApiException(HttpStatus.BAD_GATEWAY, cause);

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
            Assertions.assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("NotFoundException Tests")
    class NotFoundExceptionTests {

        @Test
        @DisplayName("Should create NotFoundException with default message")
        void shouldCreateNotFoundExceptionWithDefaultMessage() {
            NotFoundException exception = new NotFoundException();

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            Assertions.assertThat(exception.getMessage()).isEqualTo("Not Found");
        }

        @Test
        @DisplayName("Should create NotFoundException with custom message")
        void shouldCreateNotFoundExceptionWithCustomMessage() {
            NotFoundException exception = new NotFoundException("User not found");

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            Assertions.assertThat(exception.getMessage()).isEqualTo("User not found");
        }

        @Test
        @DisplayName("Should create NotFoundException with message and cause")
        void shouldCreateNotFoundExceptionWithMessageAndCause() {
            Throwable cause = new RuntimeException("DB error");

            NotFoundException exception = new NotFoundException("Resource not found", cause);

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            Assertions.assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("Should create NotFoundException with cause only")
        void shouldCreateNotFoundExceptionWithCauseOnly() {
            Throwable cause = new RuntimeException("DB error");

            NotFoundException exception = new NotFoundException(cause);

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            Assertions.assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("ConflictException Tests")
    class ConflictExceptionTests {

        @Test
        @DisplayName("Should create ConflictException with default message")
        void shouldCreateConflictExceptionWithDefaultMessage() {
            ConflictException exception = new ConflictException();

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.CONFLICT);
            Assertions.assertThat(exception.getMessage()).isEqualTo("Conflict");
        }

        @Test
        @DisplayName("Should create ConflictException with custom message")
        void shouldCreateConflictExceptionWithCustomMessage() {
            ConflictException exception = new ConflictException("Resource already exists");

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.CONFLICT);
            Assertions.assertThat(exception.getMessage()).isEqualTo("Resource already exists");
        }

        @Test
        @DisplayName("Should create ConflictException with message and cause")
        void shouldCreateConflictExceptionWithMessageAndCause() {
            Throwable cause = new RuntimeException("Duplicate key");

            ConflictException exception = new ConflictException("Duplicate resource", cause);

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.CONFLICT);
            Assertions.assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("ForbiddenException Tests")
    class ForbiddenExceptionTests {

        @Test
        @DisplayName("Should create ForbiddenException with default message")
        void shouldCreateForbiddenExceptionWithDefaultMessage() {
            ForbiddenException exception = new ForbiddenException();

            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            Assertions.assertThat(exception.getMessage()).isEqualTo("Forbidden");
        }

        @Test
        @DisplayName("Should create ForbiddenException with custom message (known bug: returns CONFLICT)")
        void shouldCreateForbiddenExceptionWithCustomMessage() {
            ForbiddenException exception = new ForbiddenException("Access denied");

            Assertions.assertThat(exception.getMessage()).isEqualTo("Access denied");
            Assertions.assertThat(exception.getResponseStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("Exception Inheritance Tests")
    class ExceptionInheritanceTests {

        @Test
        @DisplayName("NotFoundException should extend ApiException")
        void notFoundExceptionShouldExtendApiException() {
            Assertions.assertThat(new NotFoundException()).isInstanceOf(ApiException.class);
        }

        @Test
        @DisplayName("ConflictException should extend ApiException")
        void conflictExceptionShouldExtendApiException() {
            Assertions.assertThat(new ConflictException()).isInstanceOf(ApiException.class);
        }

        @Test
        @DisplayName("ForbiddenException should extend ApiException")
        void forbiddenExceptionShouldExtendApiException() {
            Assertions.assertThat(new ForbiddenException()).isInstanceOf(ApiException.class);
        }

        @Test
        @DisplayName("ApiException should extend RuntimeException")
        void apiExceptionShouldExtendRuntimeException() {
            Assertions.assertThat(new ApiException()).isInstanceOf(RuntimeException.class);
        }
    }
}
