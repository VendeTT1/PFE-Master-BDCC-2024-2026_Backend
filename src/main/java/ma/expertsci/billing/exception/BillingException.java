package ma.expertsci.billing.exception;

/**
 * Thrown for business-rule violations inside the billing module
 * (e.g. trying to pay for a free plan, duplicate initiation, etc.)
 */
public class BillingException extends RuntimeException {

    private final String errorCode;

    public BillingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}