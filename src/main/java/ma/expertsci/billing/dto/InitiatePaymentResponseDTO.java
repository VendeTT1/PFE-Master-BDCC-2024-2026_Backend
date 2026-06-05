package ma.expertsci.billing.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Returned to the frontend after your backend successfully calls CinetPay /v2/payment.
 *
 * The frontend uses `paymentToken` to open the Seamless popup:
 *   CinetPaySeamless.open({ paymentToken })
 *
 * The frontend should store `transactionId` to poll /api/billing/status/{transactionId}
 * after the popup closes.
 */
@Data
@Builder
public class InitiatePaymentResponseDTO {

    private String transactionId;    // your internal UUID
    private String paymentToken;     // CinetPay token for the Seamless SDK
    private String paymentUrl;       // fallback redirect URL (for non-seamless / iOS)
    private int amount;
    private String currency;
    private String planType;
}