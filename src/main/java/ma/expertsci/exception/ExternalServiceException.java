package ma.expertsci.exception;

import ma.expertsci.instances.exception.InstanceErrorCodes;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an external dependency (Odoo, Docker, SMTP, ...) fails. Maps
 * to HTTP 502 — we acted as a gateway to a service that misbehaved.
 */
public class ExternalServiceException extends BaseException {

    public ExternalServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, "EXTERNAL_SERVICE_ERROR", message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "EXTERNAL_SERVICE_ERROR", message, cause);
    }

    public ExternalServiceException(String errorCode, String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, errorCode, message, cause);
    }

}
