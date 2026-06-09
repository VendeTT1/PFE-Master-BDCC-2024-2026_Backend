package ma.expertsci.billing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CinetPayInitRequest {
    @JsonProperty("currency")
    private String currency;

    @JsonProperty("merchant_transaction_id")
    private String merchantTransactionId;

    @JsonProperty("amount")
    private int amount;

    @JsonProperty("designation")
    private String designation;

    @JsonProperty("lang")
    private String lang;

    @JsonProperty("channel")
    private String channel;   // PUSH | MOBILE_MONEY | CREDIT_CARD | ALL

    @JsonProperty("notify_url")
    private String notifyUrl;

    @JsonProperty("return_url")
    private String returnUrl;

    @JsonProperty("success_url")
    private String successUrl;

    @JsonProperty("failed_url")
    private String failedUrl;

    @JsonProperty("metadata")
    private String metadata;

    @JsonProperty("client_first_name")
    private String clientFirstName;

    @JsonProperty("client_last_name")
    private String clientLastName;

    @JsonProperty("client_email")
    private String clientEmail;

    @JsonProperty("client_phone_number")
    private String clientPhoneNumber;
}