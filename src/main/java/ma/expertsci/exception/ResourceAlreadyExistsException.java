package ma.expertsci.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when trying to create an entity that conflicts with an existing one
 * (duplicate email, duplicate company name, ...). Maps to HTTP 409.
 */
public class ResourceAlreadyExistsException extends BaseException {

    public ResourceAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS", message);
    }

    public ResourceAlreadyExistsException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }
}
