package ma.expertsci.instances.exception;

/**
 * Error codes for the instances module.
 */
public class InstanceErrorCodes {

    private InstanceErrorCodes() {}

    // ── Resource ──────────────────────────────────────────────────────────────
    public static final String USER_NOT_FOUND          = "USER_NOT_FOUND";
    public static final String INSTANCE_NOT_FOUND      = "INSTANCE_NOT_FOUND";
    public static final String OWNER_NOT_FOUND         = "OWNER_NOT_FOUND";

    // ── Access ────────────────────────────────────────────────────────────────
    public static final String INSTANCE_ACCESS_DENIED  = "INSTANCE_ACCESS_DENIED";

    // ── Business rules ────────────────────────────────────────────────────────
    /** Thrown when a company tries to create a second instance. */
    public static final String INSTANCE_ALREADY_EXISTS = "INSTANCE_ALREADY_EXISTS";

    /** Thrown when a user tries to create or access an instance without an active subscription. */
    public static final String SUBSCRIPTION_REQUIRED   = "SUBSCRIPTION_REQUIRED";

    // ── Docker ────────────────────────────────────────────────────────────────
    public static final String DOCKER_ERROR            = "DOCKER_ERROR";
    public static final String DOCKER_START_FAILED     = "DOCKER_START_FAILED";
    public static final String DOCKER_STOP_FAILED      = "DOCKER_STOP_FAILED";
    public static final String DOCKER_RESTART_FAILED   = "DOCKER_RESTART_FAILED";

    // ── Odoo ──────────────────────────────────────────────────────────────────
    public static final String ODOO_INIT_FAILED        = "ODOO_INIT_FAILED";

    // ── Nginx ─────────────────────────────────────────────────────────────────
    public static final String NGINX_FAILED            = "NGINX_FAILED";
}