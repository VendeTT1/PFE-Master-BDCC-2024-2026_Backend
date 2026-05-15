package ma.expertsci.exception;

import org.springframework.http.HttpStatus;

/**
 * Abstract base class for every business / application exception in the project.
 *
 * Carries the HTTP status the response should use, a stable machine-readable
 * {@code errorCode}, and the human-readable {@code message}. All subclasses are
 * unchecked so they don't pollute method signatures and integrate naturally
 * with Spring's transactional rollback semantics.
 *
 * Every subclass is converted to an {@link ApiErrorResponse} by
 * {@link GlobalExceptionHandler}.
 */
public abstract class BaseException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected BaseException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    protected BaseException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
