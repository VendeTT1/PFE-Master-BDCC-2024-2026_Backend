package ma.expertsci.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ma.expertsci.subscriptions.entities.PlanType;

/**
 * Sent by the authenticated frontend user when they click "Subscribe to plan X".
 */
@Data
public class InitiatePaymentRequestDTO {

    @NotNull(message = "Plan type is required")
    private PlanType planType;

    // Optional: customer phone for Mobile Money pre-fill
    private String customerPhone;

    // Optional: customer country code e.g. "SN", "CI", "CM"
    private String customerCountry;
}