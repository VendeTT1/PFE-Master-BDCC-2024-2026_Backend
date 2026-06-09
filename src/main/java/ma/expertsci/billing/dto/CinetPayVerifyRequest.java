package ma.expertsci.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Body sent to POST https://api-checkout.cinetpay.com/v2/payment/check
 * to verify the real status of a transaction after a webhook callback.
 *
 * CinetPay intentionally does NOT send payment status in the webhook body
 * to prevent man-in-the-middle attacks. You MUST call this endpoint yourself.
 */
@Data
@Builder
public class CinetPayVerifyRequest {

    @JsonProperty("merchant_transaction_id")
    private String merchantTransactionId;
}