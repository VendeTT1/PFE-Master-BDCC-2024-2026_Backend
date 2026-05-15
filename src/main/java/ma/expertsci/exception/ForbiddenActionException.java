package ma.expertsci.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the caller is authenticated but not allowed to perform the
 * requested action (e.g. modifying a user from another company, deactivating
 * an OWNER). Maps to HTTP 403.
 */
public class ForbiddenActionException extends BaseException {

    public ForbiddenActionException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN_ACTION", message);
    }

    public ForbiddenActionException(String errorCode, String message) {
        super(HttpStatus.FORBIDDEN, errorCode, message);
    }
}
