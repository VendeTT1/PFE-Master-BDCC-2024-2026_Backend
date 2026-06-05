package ma.expertsci.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Mapped from the JSON response of POST https://api-checkout.cinetpay.com/v2/payment
 *
 * Success response shape:
 * {
 *   "code": "201",
 *   "message": "CREATED",
 *   "description": "Transaction created with success",
 *   "data": {
 *     "payment_token": "...",
 *     "payment_url": "https://checkout.cinetpay.com/payment/..."
 *   },
 *   "api_response_id": "..."
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinetPayInitResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("description")
    private String description;

    @JsonProperty("api_response_id")
    private String apiResponseId;

    @JsonProperty("data")
    private PaymentData data;

    public boolean isSuccess() {
        return "201".equals(code);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentData {

        @JsonProperty("payment_token")
        private String paymentToken;

        @JsonProperty("payment_url")
        private String paymentUrl;
    }
}