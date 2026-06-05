package ma.expertsci.billing.dto;

import lombok.Builder;
import lombok.Data;
import ma.expertsci.billing.entity.PaymentStatus;
import ma.expertsci.subscriptions.entities.PlanType;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentHistoryDTO {

    private Long id;
    private String transactionId;
    private PlanType planType;
    private int amount;
    private String currency;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}