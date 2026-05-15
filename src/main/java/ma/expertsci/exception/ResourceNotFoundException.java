package ma.expertsci.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a domain entity (user, company, invitation, instance, ...)
 * cannot be located. Maps to HTTP 404.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String errorCode, String message) {
        super(HttpStatus.NOT_FOUND, errorCode, message);
    }
}
