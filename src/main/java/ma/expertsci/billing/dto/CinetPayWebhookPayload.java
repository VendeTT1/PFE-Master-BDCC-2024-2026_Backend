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

    /**
     * Corresponds to the transaction_id you sent at initiation.
     * This is the key to look up your PaymentTransaction record.
     */
    @JsonProperty("cpm_trans_id")
    private String cpmTransId;

    // Extra fields captured for logging — not used for business logic
    @JsonProperty("cpm_site_id")
    private String cpmSiteId;

    @JsonProperty("cpm_result")
    private String cpmResult;

    @JsonProperty("cpm_amount")
    private String cpmAmount;
}