package ma.expertsci.billing.dto;

import lombok.Builder;
import lombok.Data;
import ma.expertsci.subscriptions.entities.PlanType;
import ma.expertsci.subscriptions.entities.SubscriptionStatus;

import java.time.LocalDateTime;

/**
 * The single source of truth for "what plan is this user on right now".
 * Returned by GET /api/billing/my-plan.
 *
 * Combines:
 *  - The active Subscription record (plan type, end date, status)
 *  - Computed fields (daysRemaining, canUpgradeTo)
 *
 * Billing owns this view — SubscriptionService handles raw subscription
 * management, BillingService handles what the user sees on their billing page.
 */
@Data
@Builder
public class BillingPlanResponseDTO {

    private String companyName;

    /** Current active plan */
    private String planType;
    private String planLabel;

    /** Subscription status: ACTIVE, EXPIRED, SUSPENDED, PENDING_PAYMENT */
    private String status;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    /** How many days are left — null if no end date or already expired */
    private Long daysRemaining;

    /** Whether the subscription is currently usable */
    private boolean active;

    /** Price of current plan in XOF */
    private int currentPlanPrice;

    /**
     * PLAN_ORDER index of current plan.
     * Frontend uses this to determine which plans are upgrades vs downgrades.
     * TRIAL=0, PREMIUM=1, ENTERPRISE=2
     */
    private int planOrder;
}