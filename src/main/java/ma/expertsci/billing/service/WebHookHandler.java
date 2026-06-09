package ma.expertsci.billing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.expertsci.billing.dto.CinetPayVerifyResponse;
import ma.expertsci.billing.dto.CinetPayWebhookPayload;
import ma.expertsci.billing.entity.PaymentStatus;
import ma.expertsci.billing.entity.PaymentTransaction;
import ma.expertsci.billing.repository.PaymentTransactionRepository;
import ma.expertsci.subscriptions.service.SubscriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class WebHookHandler {

    private final CinetPayClientService cinetPayClientService;
    private final PaymentTransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public void handle(CinetPayWebhookPayload payload) {

        String transactionId = payload.getMerchantTransactionId();
        log.info("[Webhook] Received callback | transactionId={}", transactionId);

        if (transactionId == null || transactionId.isBlank()) {
            log.warn("[Webhook] Missing transaction id — ignoring.");
            return;
        }

        PaymentTransaction transaction = transactionRepository
                .findByTransactionId(transactionId).orElse(null);

        if (transaction == null) {
            log.warn("[Webhook] No transaction found for id={}", transactionId);
            return;
        }

        // Idempotency — already processed
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            log.info("[Webhook] Already SUCCESS | transactionId={} — skipping.", transactionId);
            return;
        }

        // Always verify with CinetPay — never trust webhook payload alone
        CinetPayVerifyResponse verify;
        try {
            verify = cinetPayClientService.verifyTransaction(transactionId);
        } catch (Exception ex) {
            log.error("[Webhook] Verification failed | txId={} error={}", transactionId, ex.getMessage());
            return; // leave PENDING, next webhook retry will try again
        }

        if (verify.isAccepted()) {
            handleAccepted(transaction, verify);
        } else if (verify.isWaitingForCustomer()) {
            handleWaiting(transaction);
        } else {
            handleFailed(transaction, verify);
        }
    }

    private void handleAccepted(PaymentTransaction transaction, CinetPayVerifyResponse verify) {
        log.info("[Webhook] ACCEPTED | txId={} plan={}",
                transaction.getTransactionId(), transaction.getPlanType());

        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setCinetpayResponseCode(verify.getStatus());
        transaction.setPaidAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        try {
            subscriptionService.UpgradeSubscriptionForPlan(
                    transaction.getCompany(), transaction.getPlanType());
            log.info("[Webhook] Subscription upgraded | company={} plan={}",
                    transaction.getCompany().getName(), transaction.getPlanType());
        } catch (Exception ex) {
            log.error("[Webhook] CRITICAL: Payment succeeded but subscription upgrade FAILED | " +
                            "txId={} company={} plan={} error={}",
                    transaction.getTransactionId(),
                    transaction.getCompany().getName(),
                    transaction.getPlanType(),
                    ex.getMessage());
        }
    }

    private void handleWaiting(PaymentTransaction transaction) {
        log.info("[Webhook] WAITING_FOR_CUSTOMER | txId={}", transaction.getTransactionId());
        transaction.setStatus(PaymentStatus.WAITING_CUSTOMER);
        transactionRepository.save(transaction);
    }

    private void handleFailed(PaymentTransaction transaction, CinetPayVerifyResponse verify) {
        log.info("[Webhook] FAILED | txId={} status={} reason={}",
                transaction.getTransactionId(), verify.getStatus(), verify.getErrorMessage());
        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setCinetpayResponseCode(verify.getStatus());
        transactionRepository.save(transaction);
    }
}