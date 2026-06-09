package ma.expertsci.billing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.expertsci.billing.config.CinetPayConfig;
import ma.expertsci.billing.dto.*;
import ma.expertsci.billing.exception.CinetPayApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CinetPayClientService {

    private final RestTemplate restTemplate;
    private final CinetPayConfig config;
    private final CinetPayTokenService tokenService;

    // ── POST /v1/payment ─────────────────────────────────────────────────────

    public CinetPayInitResponse initiatePayment(CinetPayInitRequest request) {
        String url = config.getBaseUrl() + "/payment";
        log.info("[CinetPay] Initiating payment | txId={} amount={} currency={}",
                request.getMerchantTransactionId(), request.getAmount(), request.getCurrency());

        return executeWithRetry(() -> {
            HttpEntity<CinetPayInitRequest> entity = new HttpEntity<>(request, buildAuthHeaders());
            ResponseEntity<CinetPayInitResponse> response =
                    restTemplate.postForEntity(url, entity, CinetPayInitResponse.class);

            CinetPayInitResponse body = response.getBody();
            if (body == null) {
                throw new CinetPayApiException("CINETPAY_NULL_RESPONSE",
                        "CinetPay returned null for payment initiation: " + request.getMerchantTransactionId());
            }
            if (!body.isSuccess()) {
                log.error("[CinetPay] Initiation failed | code={} status={}",
                        body.getCode(), body.getStatus());
                throw new CinetPayApiException("CINETPAY_INIT_FAILED",
                        "CinetPay initiation failed: [" + body.getCode() + "] " + body.getStatus());
            }

            log.info("[CinetPay] Payment initiated | txId={} paymentToken={}",
                    request.getMerchantTransactionId(), body.getPaymentToken());
            return body;
        }, "payment initiation for " + request.getMerchantTransactionId());
    }

    // ── GET /v1/payment/{merchant_transaction_id} ─────────────────────────────

    public CinetPayVerifyResponse verifyTransaction(String transactionId) {
        String url = config.getBaseUrl() + "/payment/" + transactionId;
        log.info("[CinetPay] Verifying transaction | txId={}", transactionId);

        return executeWithRetry(() -> {
            // GET with Authorization header only — no request body
            HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders());
            ResponseEntity<CinetPayVerifyResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, CinetPayVerifyResponse.class);

            CinetPayVerifyResponse body = response.getBody();
            if (body == null) {
                throw new CinetPayApiException("CINETPAY_NULL_VERIFY_RESPONSE",
                        "CinetPay returned null for verification: " + transactionId);
            }

            log.info("[CinetPay] Verification result | txId={} status={}",
                    transactionId, body.getStatus());
            return body;
        }, "verification of " + transactionId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokenService.getValidToken());
        return headers;
    }

    private <T> T executeWithRetry(ApiCall<T> call, String description) {
        try {
            return call.execute();
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.warn("[CinetPay] 401 on {} — invalidating token and retrying once.", description);
            tokenService.invalidateToken();
            try {
                return call.execute();
            } catch (RestClientException retryEx) {
                log.error("[CinetPay] Retry also failed for {}: {}", description, retryEx.getMessage());
                throw new CinetPayApiException("CINETPAY_AUTH_RETRY_FAILED",
                        "CinetPay call failed after token refresh: " + retryEx.getMessage());
            }
        } catch (RestClientException ex) {
            log.error("[CinetPay] HTTP error on {}: {}", description, ex.getMessage());
            throw new CinetPayApiException("CINETPAY_HTTP_ERROR",
                    "CinetPay HTTP error during " + description + ": " + ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T execute();
    }
}