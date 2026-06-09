package ma.expertsci.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * CinetPay POSTs this to your notify_url after each payment status change.
 *
 * IMPORTANT: Do NOT trust cpm_result from this payload for subscription activation.
 * Always call /v2/payment/check to get the real verified status.
 *
 * The only field you need from this payload is cpm_trans_id — use it
 * to look up your PaymentTransaction and then call the verify endpoint.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinetPayWebhookPayload {
    @JsonProperty("merchant_transaction_id")
    private String merchantTransactionId;

//    @JsonProperty("status")
//    private String status;
//
//    @JsonProperty("amount")
//    private String amount;
}