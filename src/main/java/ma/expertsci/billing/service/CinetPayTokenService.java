package ma.expertsci.billing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.expertsci.billing.config.CinetPayConfig;
import ma.expertsci.billing.dto.CinetPayAuthRequestDTO;
import ma.expertsci.billing.dto.CinetPayAuthResponseDTO;
import ma.expertsci.billing.exception.CinetPayApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class CinetPayTokenService {

    private final RestTemplate restTemplate;
    private final CinetPayConfig config;

    private static final int EXPIRY_BUFFER_SECONDS = 60;

    private String cachedToken = null;
    private Instant tokenExpiresAt = Instant.EPOCH;

    private final ReentrantLock lock = new ReentrantLock();

    public String getValidToken() {
        if (isTokenFresh()) {
            return cachedToken;
        }
        lock.lock();
        try {
            if (isTokenFresh()) {
                return cachedToken;
            }
            log.info("[CinetPay Token] Token absent or expiring — fetching new token.");
            refreshToken();
            return cachedToken;
        } finally {
            lock.unlock();
        }
    }

    private boolean isTokenFresh() {
        return cachedToken != null
                && Instant.now().isBefore(tokenExpiresAt.minusSeconds(EXPIRY_BUFFER_SECONDS));
    }

    private void refreshToken() {
        String loginUrl = config.getBaseUrl() + "/oauth/login";

        CinetPayAuthRequestDTO request = CinetPayAuthRequestDTO.builder()
                .apiKey(config.getApiKey())
                .apiPassword(config.getApiPassword())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            CinetPayAuthResponseDTO response = restTemplate.postForObject(
                    loginUrl,
                    new HttpEntity<>(request, headers),
                    CinetPayAuthResponseDTO.class
            );


            if (response == null || !response.isSuccess()) {
                String msg = response != null ? response.getStatus() : "null response";
                log.error("[CinetPay Token] OAuth login failed: {}", msg);
                throw new CinetPayApiException("CINETPAY_AUTH_FAILED",
                        "CinetPay OAuth login failed: " + msg);
            }

            // Fields are now directly on the response object — no .getData() needed
            cachedToken = response.getAccessToken();
            int expiresIn = response.getExpiresIn();
            tokenExpiresAt = Instant.now().plusSeconds(expiresIn);

            log.info("[CinetPay Token] Token obtained. Expires in {}s.", expiresIn);

        } catch (RestClientException ex) {
            log.error("[CinetPay Token] HTTP error during OAuth login: {}", ex.getMessage());
            throw new CinetPayApiException("CINETPAY_AUTH_HTTP_ERROR",
                    "Failed to reach CinetPay OAuth endpoint: " + ex.getMessage());
        }
    }

    public void invalidateToken() {
        lock.lock();
        try {
            log.warn("[CinetPay Token] Token invalidated — will refresh on next call.");
            cachedToken = null;
            tokenExpiresAt = Instant.EPOCH;
        } finally {
            lock.unlock();
        }
    }
}