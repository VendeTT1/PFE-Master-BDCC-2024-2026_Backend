package ma.expertsci.instances.exception;

    /**
     * Stable, machine-readable error codes for the instances module.
     * <p>
     * Used as the {@code errorCode} field in {@link ma.expertsci.exception.ApiErrorResponse}.
     * Keep them ALL_CAPS_UNDERSCORE so the frontend can switch on them reliably.
     */
    public final class InstanceErrorCodes {

        private InstanceErrorCodes() {}

        // ── Not-found ────────────────────────────────────────────────────────────
        /** No user row matches the given email. */
        public static final String USER_NOT_FOUND           = "USER_NOT_FOUND";

        /** No instance row matches the given id or company name. */
        public static final String INSTANCE_NOT_FOUND       = "INSTANCE_NOT_FOUND";

        /** No OWNER-role user found for the given instance/company name. */
        public static final String OWNER_NOT_FOUND          = "OWNER_NOT_FOUND";

        // ── Authorization ────────────────────────────────────────────────────────
        /** Caller's company does not match the instance's company. */
        public static final String INSTANCE_ACCESS_DENIED   = "INSTANCE_ACCESS_DENIED";

        // ── External-service / infrastructure errors ─────────────────────────────
        /** Docker container creation or startup failed. */
        public static final String DOCKER_START_FAILED      = "DOCKER_START_FAILED";

        /** Docker container stop command failed. */
        public static final String DOCKER_STOP_FAILED       = "DOCKER_STOP_FAILED";

        /** Docker container restart failed. */
        public static final String DOCKER_RESTART_FAILED    = "DOCKER_RESTART_FAILED";

        /** Odoo initialization (--stop-after-init) exited with a non-zero code. */
        public static final String ODOO_INIT_FAILED         = "ODOO_INIT_FAILED";

        /** Nginx configuration generation or reload failed. */
        public static final String NGINX_FAILED             = "NGINX_FAILED";

        /** General Docker / infrastructure error not covered above. */
        public static final String DOCKER_ERROR             = "DOCKER_ERROR";
    }
