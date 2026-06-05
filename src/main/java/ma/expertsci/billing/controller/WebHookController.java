package ma.expertsci.billing.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.expertsci.billing.dto.CinetPayWebhookPayload;
import ma.expertsci.billing.service.WebHookHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives webhook callbacks from CinetPay.
 *
 * SECURITY NOTE:
 * This endpoint MUST be excluded from your JWT security filter.
 * CinetPay calls it as an anonymous server-to-server request.
 * Add it to your SecurityFilterChain:
 *
 *   .requestMatchers("/api/webhook/cinetpay").permitAll()
 *
 * IMPORTANT:
 * This endpoint must ALWAYS return HTTP 200, even on internal errors.
 * If it returns non-200, CinetPay will retry the webhook indefinitely,
 * which can cause duplicate subscription activations.
 *
 * All safety against duplication is handled inside WebhookHandler via
 * idempotency checks on the transaction status.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebHookController {

    private final WebHookHandler webhookHandler;

    @PostMapping("/cinetpay")
    public ResponseEntity<Void> handleCinetPayWebhook(
            @RequestBody(required = false) CinetPayWebhookPayload payload) {

        // Log the raw callback for debugging (especially useful in early integration)
        log.info("[Webhook] CinetPay callback received | transId={}",
                payload != null ? payload.getCpmTransId() : "null");

        try {
            if (payload != null) {
                webhookHandler.handle(payload);
            } else {
                log.warn("[Webhook] Received empty payload from CinetPay — ignoring.");
            }
        } catch (Exception ex) {
            // Catch everything — we must return 200 regardless
            log.error("[Webhook] Unhandled exception in webhook handler: {}", ex.getMessage(), ex);
        }

        // Always 200 — see class javadoc
        return ResponseEntity.ok().build();
    }
}