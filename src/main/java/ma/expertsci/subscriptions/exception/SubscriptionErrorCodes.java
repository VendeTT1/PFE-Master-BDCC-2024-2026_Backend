package ma.expertsci.subscriptions.exception;

public class SubscriptionErrorCodes {
    /** No subscription row exists for the given company. */
    public static final String SUBSCRIPTION_NOT_FOUND   = "SUBSCRIPTION_NOT_FOUND";

    /** The authenticated user could not be resolved. */
    public static final String USER_NOT_FOUND           = "USER_NOT_FOUND";

    /** The user is not associated with any company. */
    public static final String COMPANY_NOT_FOUND        = "COMPANY_NOT_FOUND";

    // ── Business-rule violations ─────────────────────────────────────────────
    /** The subscription is currently suspended by an admin. */
    public static final String SUBSCRIPTION_SUSPENDED   = "SUBSCRIPTION_SUSPENDED";

    /** The subscription end-date has passed. */
    public static final String SUBSCRIPTION_EXPIRED     = "SUBSCRIPTION_EXPIRED";

    /** The subscription is in a state other than ACTIVE. */
    public static final String SUBSCRIPTION_INACTIVE    = "SUBSCRIPTION_INACTIVE";

    /** The requested plan type is unknown / unsupported. */
    public static final String UNKNOWN_PLAN_TYPE        = "UNKNOWN_PLAN_TYPE";
}
