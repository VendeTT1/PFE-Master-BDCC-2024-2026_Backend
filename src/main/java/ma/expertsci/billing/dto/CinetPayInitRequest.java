package ma.expertsci.billing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Maps exactly to the JSON body expected by:
 *   POST https://api-checkout.cinetpay.com/v2/payment
 *
 * Required fields: apikey, site_id, transaction_id, amount, currency,
 *                  description, notify_url, return_url
 *
 * Optional customer fields are needed to enable credit card payment universe.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CinetPayInitRequest {

    @JsonProperty("apikey")
    private String apikey;

    @JsonProperty("site_id")
    private String siteId;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("amount")
    private int amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("description")
    private String description;

    @JsonProperty("notify_url")
    private String notifyUrl;

    @JsonProperty("return_url")
    private String returnUrl;

    @JsonProperty("channels")
    private String channels;

    @JsonProperty("lang")
    private String lang;

    @JsonProperty("metadata")
    private String metadata;  // we'll store companyId here for traceability

    // Optional — required for credit card universe
    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_surname")
    private String customerSurname;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("customer_phone_number")
    private String customerPhoneNumber;

    @JsonProperty("customer_country")
    private String customerCountry;
}