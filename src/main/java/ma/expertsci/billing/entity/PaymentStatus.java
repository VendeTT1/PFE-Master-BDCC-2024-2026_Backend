package ma.expertsci.billing.entity;

/**
 * Lifecycle of a single payment attempt.
 *
 * PENDING          → created locally, waiting for user to pay in the popup
 * SUCCESS          → CinetPay verified the payment as ACCEPTED; subscription upgraded
 * FAILED           → CinetPay returned REFUSED or an error
 * CANCELLED        → user closed the popup without paying (set on frontend callback)
 * WAITING_CUSTOMER → operator requires push confirmation from user (e.g. Mobile Money prompt)
 *                    Do NOT treat this as a final failure — wait for the follow-up webhook
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    WAITING_CUSTOMER
}