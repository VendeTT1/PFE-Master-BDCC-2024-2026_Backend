package ma.expertsci.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when authentication credentials (email/password, reset token, refresh
 * token, ...) are invalid. Maps to HTTP 401.
 */
public class InvalidCredentialsException extends BaseException {

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message);
    }

    public InvalidCredentialsException(String errorCode, String message) {
        super(HttpStatus.UNAUTHORIZED, errorCode, message);
    }
}
