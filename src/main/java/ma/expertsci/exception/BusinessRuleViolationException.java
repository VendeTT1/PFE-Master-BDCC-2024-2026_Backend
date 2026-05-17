package ma.expertsci.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is syntactically valid but breaks a business rule
 * (trial user-limit reached, password reset token expired, subscription
 * suspended, ...). Maps to HTTP 400.
 */
public class BusinessRuleViolationException extends BaseException {

    public BusinessRuleViolationException(String message) {
        super(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", message);
    }

    public BusinessRuleViolationException(String errorCode, String message) {
        super(HttpStatus.BAD_REQUEST, errorCode, message);
    }
}
