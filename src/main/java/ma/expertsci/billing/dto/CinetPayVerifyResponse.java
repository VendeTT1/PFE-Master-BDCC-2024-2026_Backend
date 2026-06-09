package ma.expertsci.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Flat response from POST https://api.cinetpay.net/v1/payment/check
 *
 * {
 *   "code": 200,
 *   "status": "ACCEPTED" | "REFUSED" | "CANCELLED" | "WAITING_FOR_CUSTOMER",
 *   "merchant_transaction_id": "...",
 *   "amount": "15000",
 *   "currency": "XOF",
 *   "payment_method": "MOBILE_MONEY",
 *   "error_message": null
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinetPayVerifyResponse {

    @JsonProperty("code")
    private int code;

    /** ACCEPTED | REFUSED | CANCELLED | WAITING_FOR_CUSTOMER */
    @JsonProperty("status")
    private String status;

    @JsonProperty("merchant_transaction_id")
    private String merchantTransactionId;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("error_message")
    private String errorMessage;

    public boolean isAccepted() {
        return "ACCEPTED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    }

    public boolean isWaitingForCustomer() {
        return "WAITING_FOR_CUSTOMER".equalsIgnoreCase(status);
    }
}