package ma.expertsci.billing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.expertsci.billing.config.CinetPayConfig;
import ma.expertsci.billing.dto.CinetPayInitRequest;
import ma.expertsci.billing.dto.CinetPayInitResponse;
import ma.expertsci.billing.dto.CinetPayVerifyRequest;
import ma.expertsci.billing.dto.CinetPayVerifyResponse;
import ma.expertsci.billing.exception.CinetPayApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin HTTP wrapper around the CinetPay REST API.
 * Responsible for: initiating a payment and verifying a transaction.
 * All business logic lives in BillingService — this class only does I/O.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CinetPayClientService {

    private final RestTemplate restTemplate;
    private final CinetPayConfig config;

    /**
     * Step 1 of the payment flow.
     * Calls POST /v2/payment to register a new transaction with CinetPay.
     * On success, returns a payment_token to hand off to the Seamless SDK.
     *
     * @throws CinetPayApiException if CinetPay returns a non-201 code or the call fails
     */
    public CinetPayInitResponse initiatePayment(CinetPayInitRequest request) {
        log.info("[CinetPay] Initiating payment | transactionId={} amount={} currency={}",
                request.getTransactionId(), request.getAmount(), request.getCurrency());

        HttpHeaders headers = buildJsonHeaders();
        HttpEntity<CinetPayInitRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<CinetPayInitResponse> response = restTemplate.postForEntity(
                    config.getPaymentUrl(),
                    entity,
                    CinetPayInitResponse.class
            );

            CinetPayInitResponse body = response.getBody();

            if (body == null) {
                throw new CinetPayApiException("CINETPAY_NULL_RESPONSE",
                        "CinetPay returned a null response for transaction: " + request.getTransactionId());
            }

            if (!body.isSuccess()) {
                log.error("[CinetPay] Initiation failed | code={} message={} description={}",
                        body.getCode(), body.getMessage(), body.getDescription());
                throw new CinetPayApiException("CINETPAY_INIT_FAILED",
                        "CinetPay initiation failed: [" + body.getCode() + "] " + body.getDescription());
            }

            log.info("[CinetPay] Payment initiated successfully | transactionId={} token={}",
                    request.getTransactionId(), body.getData().getPaymentToken());

            return body;

        } catch (RestClientException ex) {
            log.error("[CinetPay] HTTP error during initiation | transactionId={} error={}",
                    request.getTransactionId(), ex.getMessage());
            throw new CinetPayApiException("CINETPAY_HTTP_ERROR",
                    "Failed to reach CinetPay API: " + ex.getMessage());
        }
    }

    /**
     * Step 2 of the payment flow — called ONLY from the webhook handler.
     * Calls POST /v2/payment/check to get the verified real status.
     *
     * CinetPay explicitly does NOT send the payment status in webhook callbacks
     * to prevent spoofing. This call is mandatory before activating a subscription.
     *
     * @param transactionId your internal transaction_id
     * @throws CinetPayApiException if the call fails
     */
    public CinetPayVerifyResponse verifyTransaction(String transactionId) {
        log.info("[CinetPay] Verifying transaction | transactionId={}", transactionId);

        CinetPayVerifyRequest request = CinetPayVerifyRequest.builder()
                .transactionId(transactionId)
                .siteId(config.getSiteId())
                .apikey(config.getApiKey())
                .build();

        HttpHeaders headers = buildJsonHeaders();
        HttpEntity<CinetPayVerifyRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<CinetPayVerifyResponse> response = restTemplate.postForEntity(
                    config.getVerifyUrl(),
                    entity,
                    CinetPayVerifyResponse.class
            );

            CinetPayVerifyResponse body = response.getBody();

            if (body == null) {
                throw new CinetPayApiException("CINETPAY_NULL_VERIFY_RESPONSE",
                        "CinetPay returned null for verification of transaction: " + transactionId);
            }

            log.info("[CinetPay] Verification result | transactionId={} status={}",
                    transactionId,
                    body.getData() != null ? body.getData().getPaymentStatus() : "null");

            return body;

        } catch (RestClientException ex) {
            log.error("[CinetPay] HTTP error during verification | transactionId={} error={}",
                    transactionId, ex.getMessage());
            throw new CinetPayApiException("CINETPAY_VERIFY_HTTP_ERROR",
                    "Failed to verify transaction with CinetPay: " + ex.getMessage());
        }
    }

    private HttpHeaders buildJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}