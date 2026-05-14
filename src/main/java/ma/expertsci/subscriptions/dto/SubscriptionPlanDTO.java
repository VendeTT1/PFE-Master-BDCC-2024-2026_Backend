package ma.expertsci.subscriptions.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionPlanDTO {
    private String code;  // e.g., "TRIAL", "PREMIUM", "ENTERPRISE"
    private String label;
    private int includedUsers;  // Number of users allowed
    private boolean paid;
}
