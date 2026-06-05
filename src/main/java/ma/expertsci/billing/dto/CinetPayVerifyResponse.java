package ma.expertsci.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Mapped from the JSON response of POST https://api-checkout.cinetpay.com/v2/payment/check
 *
 * Key field: data.payment_status
 *   "ACCEPTED"            → payment confirmed, activate subscription
 *   "REFUSED"             → payment declined
 *   "CANCELLED"           → user cancelled
 *   "WAITING_FOR_CUSTOMER"→ mobile money push sent, awaiting user approval (NOT a final state)
 *
 * Key field: data.code (also called cpm_result in older API)
 *   "00" → success/accepted
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinetPayVerifyResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private VerifyData data;

    public boolean isAccepted() {
        return data != null && "ACCEPTED".equalsIgnoreCase(data.getPaymentStatus());
    }

    public boolean isWaitingForCustomer() {
        return data != null && "WAITING_FOR_CUSTOMER".equalsIgnoreCase(data.getPaymentStatus());
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerifyData {

        @JsonProperty("payment_status")
        private String paymentStatus;   // ACCEPTED | REFUSED | CANCELLED | WAITING_FOR_CUSTOMER

        @JsonProperty("cpm_result")
        private String cpmResult;       // "00" on success

        @JsonProperty("cpm_amount")
        private String amount;

        @JsonProperty("cpm_currency")
        private String currency;

        @JsonProperty("cpm_trans_id")
        private String transactionId;

        @JsonProperty("cpm_error_message")
        private String errorMessage;

        @JsonProperty("payment_method")
        private String paymentMethod;
    }
}