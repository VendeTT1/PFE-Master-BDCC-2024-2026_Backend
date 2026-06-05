package ma.expertsci.billing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.billing.config.CinetPayConfig;
import ma.expertsci.billing.dto.*;
import ma.expertsci.billing.entity.PaymentStatus;
import ma.expertsci.billing.entity.PaymentTransaction;
import ma.expertsci.billing.exception.BillingException;
import ma.expertsci.billing.exception.CinetPayApiException;
import ma.expertsci.billing.repository.PaymentTransactionRepository;
import ma.expertsci.exception.BusinessRuleViolationException;
import ma.expertsci.exception.ResourceNotFoundException;
import ma.expertsci.subscriptions.entities.PlanType;
import ma.expertsci.subscriptions.entities.SubscriptionStatus;
import ma.expertsci.subscriptions.exception.SubscriptionErrorCodes;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import ma.expertsci.subscriptions.service.SubscriptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final CinetPayClientService cinetPayClient;
    private final CinetPayConfig cinetPayConfig;
    private final PaymentTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    // ── Initiate a payment ────────────────────────────────────────────────────

    /**
     * Called by POST /api/billing/initiate (authenticated).
     *
     * Flow:
     * 1. Resolve the authenticated user and their company
     * 2. Validate the requested plan (must be paid, must not already be on same/higher plan)
     * 3. Generate a unique transactionId and persist a PENDING record
     * 4. Call CinetPay /v2/payment to get a payment_token
     * 5. Update the record with the token and return it to the frontend
     */
    @Transactional
    public InitiatePaymentResponseDTO initiatePayment(InitiatePaymentRequestDTO request) {

        // 1. Resolve caller
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND", "No user found with email: " + email));

        if (user.getCompany() == null) {
            throw new ResourceNotFoundException(
                    "COMPANY_NOT_FOUND", "User is not associated with any company.");
        }

        // 2. Validate plan
        PlanType selectedPlan = request.getPlanType();

        if (!selectedPlan.isPaid()) {
            throw new BusinessRuleViolationException(
                    "INVALID_PLAN", "Cannot initiate payment for a free plan: " + selectedPlan.name());
        }

        // 3. Build unique transaction ID (UUIDs are safe — no special chars except hyphens, stripped below)
        String transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        // 4. Persist PENDING record before calling CinetPay
        //    If CinetPay call fails, we still have a record for audit
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionId(transactionId)
                .company(user.getCompany())
                .planType(selectedPlan)
                .amount(selectedPlan.getPriceXOF())
                .currency(cinetPayConfig.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();

        transaction = transactionRepository.save(transaction);

        // 5. Build and fire CinetPay initiation request
        CinetPayInitRequest cinetPayRequest = CinetPayInitRequest.builder()
                .apikey(cinetPayConfig.getApiKey())
                .siteId(cinetPayConfig.getSiteId())
                .transactionId(transactionId)
                .amount(selectedPlan.getPriceXOF())
                .currency(cinetPayConfig.getCurrency())
                .description(buildDescription(selectedPlan))
                .notifyUrl(cinetPayConfig.getNotifyUrl())
                .returnUrl(cinetPayConfig.getReturnUrl())
                .channels(cinetPayConfig.getChannels())
                .lang(cinetPayConfig.getLang())
                .metadata(String.valueOf(user.getCompany().getId()))
                .customerId(String.valueOf(user.getId()))
                .customerName(user.getLastName())
                .customerSurname(user.getFirstName())
                .customerEmail(user.getEmail())
                .customerPhoneNumber(request.getCustomerPhone())
                .customerCountry(request.getCustomerCountry())
                .build();

        CinetPayInitResponse cinetPayResponse;
        try {
            cinetPayResponse = cinetPayClient.initiatePayment(cinetPayRequest);
        } catch (CinetPayApiException ex) {
            // Mark the transaction as failed so it's visible in history
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setCinetpayResponseCode("INIT_ERROR");
            transactionRepository.save(transaction);
            throw ex;
        }

        // 6. Update record with the token
        transaction.setCinetpayToken(cinetPayResponse.getData().getPaymentToken());
        transactionRepository.save(transaction);

        return InitiatePaymentResponseDTO.builder()
                .transactionId(transactionId)
                .paymentToken(cinetPayResponse.getData().getPaymentToken())
                .paymentUrl(cinetPayResponse.getData().getPaymentUrl())
                .amount(selectedPlan.getPriceXOF())
                .currency(cinetPayConfig.getCurrency())
                .planType(selectedPlan.name())
                .build();
    }

    // ── Get payment status (for frontend polling) ─────────────────────────────

    public PaymentStatusResponseDTO getPaymentStatus(String transactionId) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND", "No user found with email: " + email));

        PaymentTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TRANSACTION_NOT_FOUND", "No transaction found: " + transactionId));

        // Security: only allow access to own company's transactions
        if (!transaction.getCompany().getId().equals(user.getCompany().getId())) {
            throw new BusinessRuleViolationException(
                    "FORBIDDEN_TRANSACTION", "Transaction does not belong to your company.");
        }

        return mapToStatusDTO(transaction);
    }

    // ── Billing history for the current user's company ────────────────────────

    public List<PaymentHistoryDTO> getBillingHistory() {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND", "No user found with email: " + email));

        return transactionRepository
                .findByCompanyOrderByCreatedAtDesc(user.getCompany())
                .stream()
                .map(this::mapToHistoryDTO)
                .collect(Collectors.toList());
    }

    // ── Admin: all billing history paginated ─────────────────────────────────

    public Page<PaymentHistoryDTO> getAllBillingHistoryForAdmin(Pageable pageable) {
        return transactionRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToHistoryDTO);
    }

    // ── Mark a transaction as cancelled (called from frontend on popup close) ─

    @Transactional
    public void cancelTransaction(String transactionId) {

        PaymentTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TRANSACTION_NOT_FOUND", "No transaction found: " + transactionId));

        // Only PENDING transactions can be cancelled — don't overwrite a SUCCESS
        if (transaction.getStatus() == PaymentStatus.PENDING) {
            transaction.setStatus(PaymentStatus.CANCELLED);
            transactionRepository.save(transaction);
            log.info("[Billing] Transaction cancelled by user | transactionId={}", transactionId);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildDescription(PlanType plan) {
        return "Subscription to " + plan.name() + " plan - ExpertSci SaaS";
    }

    private PaymentStatusResponseDTO mapToStatusDTO(PaymentTransaction t) {
        return PaymentStatusResponseDTO.builder()
                .transactionId(t.getTransactionId())
                .status(t.getStatus())
                .planType(t.getPlanType())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .companyName(t.getCompany().getName())
                .createdAt(t.getCreatedAt())
                .paidAt(t.getPaidAt())
                .message(resolveStatusMessage(t.getStatus()))
                .build();
    }

    private PaymentHistoryDTO mapToHistoryDTO(PaymentTransaction t) {
        return PaymentHistoryDTO.builder()
                .id(t.getId())
                .transactionId(t.getTransactionId())
                .planType(t.getPlanType())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .paidAt(t.getPaidAt())
                .build();
    }

    private String resolveStatusMessage(PaymentStatus status) {
        switch (status) {
            case SUCCESS:          return "Payment successful. Your subscription is now active.";
            case PENDING:          return "Payment is pending. Please complete payment in the popup.";
            case FAILED:           return "Payment failed. Please try again.";
            case CANCELLED:        return "Payment was cancelled.";
            case WAITING_CUSTOMER: return "Awaiting your approval on your mobile device.";
            default:               return "Unknown status.";
        }
    }
}