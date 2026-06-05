package ma.expertsci.billing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.expertsci.billing.dto.CinetPayVerifyResponse;
import ma.expertsci.billing.dto.CinetPayWebhookPayload;
import ma.expertsci.billing.entity.PaymentStatus;
import ma.expertsci.billing.entity.PaymentTransaction;
import ma.expertsci.billing.repository.PaymentTransactionRepository;
import ma.expertsci.exception.ResourceNotFoundException;
import ma.expertsci.subscriptions.service.SubscriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles the full webhook callback lifecycle from CinetPay.
 *
 * CRITICAL RULES (from CinetPay docs):
 *
 * 1. Your notify_url WILL be called multiple times for the same transaction.
 *    Always check if the transaction is already SUCCESS before doing anything.
 *
 * 2. Never trust the payload — always call /v2/payment/check to get real status.
 *
 * 3. Always return HTTP 200 to CinetPay, even on errors.
 *    Non-200 responses will cause CinetPay to retry indefinitely.
 *
 * 4. WAITING_FOR_CUSTOMER is NOT a final state — it means the mobile money
 *    push was sent. A follow-up webhook will arrive with the final status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebHookHandler {

    private final CinetPayClientService cinetPayClient;
    private final PaymentTransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public void handle(CinetPayWebhookPayload payload) {

        String transactionId = payload.getCpmTransId();
        log.info("[Webhook] Received callback | transactionId={}", transactionId);

        if (transactionId == null || transactionId.isBlank()) {
            log.warn("[Webhook] Received webhook with missing cpm_trans_id — ignoring.");
            return;
        }

        // 1. Look up the transaction
        PaymentTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElse(null);

        if (transaction == null) {
            log.warn("[Webhook] No transaction found for id={} — possibly a test ping or stale webhook.", transactionId);
            return;
        }

        // 2. Idempotency guard — if already SUCCESS, do nothing
        //    CinetPay calls notify_url multiple times; we must not re-activate
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            log.info("[Webhook] Transaction already processed as SUCCESS | transactionId={} — skipping.", transactionId);
            return;
        }

        // 3. Call CinetPay to get the REAL verified status
        CinetPayVerifyResponse verifyResponse;
        try {
            verifyResponse = cinetPayClient.verifyTransaction(transactionId);
        } catch (Exception ex) {
            log.error("[Webhook] Verification call failed | transactionId={} error={}", transactionId, ex.getMessage());
            // Don't update the transaction — leave it PENDING so the next webhook retry can try again
            return;
        }

        // 4. Handle each possible status
        if (verifyResponse.isAccepted()) {
            handleAccepted(transaction, verifyResponse);

        } else if (verifyResponse.isWaitingForCustomer()) {
            handleWaiting(transaction);

        } else {
            handleFailed(transaction, verifyResponse);
        }
    }

    // ── Accepted ─────────────────────────────────────────────────────────────

    private void handleAccepted(PaymentTransaction transaction, CinetPayVerifyResponse verifyResponse) {
        log.info("[Webhook] Payment ACCEPTED | transactionId={} plan={}",
                transaction.getTransactionId(), transaction.getPlanType());

        String responseCode = verifyResponse.getData() != null
                ? verifyResponse.getData().getCpmResult()
                : "00";

        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setCinetpayResponseCode(responseCode);
        transaction.setPaidAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Upgrade the subscription
        try {
            subscriptionService.UpgradeSubscriptionForPlan(
                    transaction.getCompany(),
                    transaction.getPlanType()
            );
            log.info("[Webhook] Subscription upgraded | company={} plan={}",
                    transaction.getCompany().getName(), transaction.getPlanType());
        } catch (Exception ex) {
            // This is a serious inconsistency: payment succeeded but subscription upgrade failed.
            // Log it loudly — ops team must investigate. The transaction is still marked SUCCESS
            // so the idempotency guard won't re-process it.
            log.error("[Webhook] CRITICAL: Payment succeeded but subscription upgrade FAILED | " +
                            "transactionId={} company={} plan={} error={}",
                    transaction.getTransactionId(),
                    transaction.getCompany().getName(),
                    transaction.getPlanType(),
                    ex.getMessage());
        }
    }

    // ── Waiting for customer (Mobile Money push sent) ─────────────────────────

    private void handleWaiting(PaymentTransaction transaction) {
        log.info("[Webhook] WAITING_FOR_CUSTOMER | transactionId={} — awaiting user mobile approval.",
                transaction.getTransactionId());

        // Update to WAITING_CUSTOMER but keep it non-final so follow-up webhook can process it
        transaction.setStatus(PaymentStatus.WAITING_CUSTOMER);
        transactionRepository.save(transaction);

        // Note: the next webhook callback from CinetPay will be either ACCEPTED or REFUSED
        // At that point this method will be called again and will go into handleAccepted or handleFailed
    }

    // ── Failed / Refused ─────────────────────────────────────────────────────

    private void handleFailed(PaymentTransaction transaction, CinetPayVerifyResponse verifyResponse) {
        String paymentStatus = verifyResponse.getData() != null
                ? verifyResponse.getData().getPaymentStatus()
                : "UNKNOWN";
        String errorMsg = verifyResponse.getData() != null
                ? verifyResponse.getData().getErrorMessage()
                : null;

        log.info("[Webhook] Payment FAILED/REFUSED | transactionId={} status={} reason={}",
                transaction.getTransactionId(), paymentStatus, errorMsg);

        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setCinetpayResponseCode(paymentStatus);
        transactionRepository.save(transaction);
    }
}