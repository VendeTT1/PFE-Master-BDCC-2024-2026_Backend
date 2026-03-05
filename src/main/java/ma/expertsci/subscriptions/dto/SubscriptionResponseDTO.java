package ma.expertsci.subscriptions.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponseDTO {

    private String planType;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
