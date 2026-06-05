package ma.expertsci.billing.dto;

import lombok.Builder;
import lombok.Data;
import ma.expertsci.billing.entity.PaymentStatus;
import ma.expertsci.subscriptions.entities.PlanType;

import java.time.LocalDateTime;

/**
 * Returned when the frontend polls GET /api/billing/status/{transactionId}
 * after the Seamless popup closes.
 */
@Data
@Builder
public class PaymentStatusResponseDTO {

    private String transactionId;
    private PaymentStatus status;
    private PlanType planType;
    private int amount;
    private String currency;
    private String companyName;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    /** Human-readable message for the frontend to display */
    private String message;
}