package ma.expertsci.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Flat response from POST https://api.cinetpay.net/v1/payment
 *
 * {
 *   "code": 200,
 *   "status": "OK",
 *   "merchant_transaction_id": "...",
 *   "notify_token": "...",
 *   "transaction_id": "...",
 *   "payment_token": "...",
 *   "payment_url": "https://secure.cinetpay.net/payment/...",
 *   "details": { "code": 2010, "status": "FAILED", ... }  ← ignore, only relevant after payment
 * }
 *
 * No nested data object — all fields are at the root level.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinetPayInitResponse {

    @JsonProperty("code")
    private int code;

    @JsonProperty("status")
    private String status;

    @JsonProperty("merchant_transaction_id")
    private String merchantTransactionId;

    @JsonProperty("notify_token")
    private String notifyToken;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("payment_token")
    private String paymentToken;

    @JsonProperty("payment_url")
    private String paymentUrl;

    public boolean isSuccess() {
        return code == 200 && paymentToken != null && !paymentToken.isBlank();
    }
}