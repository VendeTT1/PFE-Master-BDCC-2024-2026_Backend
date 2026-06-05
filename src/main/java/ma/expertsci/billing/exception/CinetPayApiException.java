package ma.expertsci.billing.exception;

/**
 * Thrown when a call to the CinetPay API fails —
 * either an HTTP error, a null response, or a non-201 response code.
 */
public class CinetPayApiException extends RuntimeException {

    private final String errorCode;

    public CinetPayApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}