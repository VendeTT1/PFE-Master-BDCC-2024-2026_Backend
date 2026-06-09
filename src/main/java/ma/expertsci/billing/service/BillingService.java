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
    private final SubscriptionService subscriptionService;

    // ── Initiate a payment ────────────────────────────────────────────────────

    @Transactional
    public InitiatePaymentResponseDTO initiatePayment(InitiatePaymentRequestDTO request) {

        // 1. Resolve authenticated caller
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND", "No user found with email: " + email));

        if (user.getCompany() == null) {
            throw new ResourceNotFoundException(
                    "COMPANY_NOT_FOUND", "User is not associated with any company.");
        }

        // 2. Validate plan — only paid plans go through the payment flow
        PlanType selectedPlan = request.getPlanType();
        if (!selectedPlan.isPaid()) {
            throw new BusinessRuleViolationException(
                    "INVALID_PLAN", "Cannot initiate payment for a free plan: " + selectedPlan.name());
        }

        // 3. Generate unique transaction ID — no special chars per CinetPay requirement
        String transactionId = "TXN" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        // 4. Persist PENDING record before calling CinetPay
        //    Ensures we have an audit trail even if the CinetPay call fails
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionId(transactionId)
                .company(user.getCompany())
                .planType(selectedPlan)
                .amount(selectedPlan.getPriceXOF())
                .currency(cinetPayConfig.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();
        transaction = transactionRepository.save(transaction);

        // 5. Build the v1/payment request body
        //    Note: no api_key or api_password here — auth is via Bearer token in the header,
        //    handled transparently by CinetPayClient → CinetPayTokenService
        CinetPayInitRequest cinetPayRequest = CinetPayInitRequest.builder()
                .currency(cinetPayConfig.getCurrency())
                .merchantTransactionId(transactionId)
                .amount(selectedPlan.getPriceXOF())
                .designation("Subscription to " + selectedPlan.name() + " plan - ExpertSci")
                .lang(cinetPayConfig.getLang())
                .channel(cinetPayConfig.getChannels())
                .notifyUrl(cinetPayConfig.getNotifyUrl())
                .returnUrl(cinetPayConfig.getReturnUrl())
                .successUrl(cinetPayConfig.getSuccessUrl())
                .failedUrl(cinetPayConfig.getFailedUrl())
                .metadata(String.valueOf(user.getCompany().getId()))
                .clientFirstName(user.getFirstName())
                .clientLastName(user.getLastName())
                .clientEmail(user.getEmail())
                .clientPhoneNumber(request.getCustomerPhone())
                .build();

        log.info("[Billing] notify_url being sent to CinetPay: {}", cinetPayConfig.getNotifyUrl());
        // 6. Call CinetPay — on failure, mark transaction FAILED and rethrow
        CinetPayInitResponse cinetPayResponse;
        try {
            cinetPayResponse = cinetPayClient.initiatePayment(cinetPayRequest);
        } catch (CinetPayApiException ex) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setCinetpayResponseCode("INIT_ERROR");
            transactionRepository.save(transaction);
            throw ex;
        }

        // 7. Update record with the paymentToken for traceability
        transaction.setCinetpayToken(cinetPayResponse.getPaymentToken());
        transactionRepository.save(transaction);

        return InitiatePaymentResponseDTO.builder()
                .transactionId(transactionId)
                .paymentToken(cinetPayResponse.getPaymentToken())
                .paymentUrl(cinetPayResponse.getPaymentUrl())
                .amount(selectedPlan.getPriceXOF())
                .currency(cinetPayConfig.getCurrency())
                .planType(selectedPlan.name())
                .build();
    }

    // ── Get payment status (frontend polling) ─────────────────────────────────

    public PaymentStatusResponseDTO getPaymentStatus(String transactionId) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND", "No user found with email: " + email));

        PaymentTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TRANSACTION_NOT_FOUND", "No transaction found: " + transactionId));

        if (!transaction.getCompany().getId().equals(user.getCompany().getId())) {
            throw new BusinessRuleViolationException(
                    "FORBIDDEN_TRANSACTION", "Transaction does not belong to your company.");
        }

        return mapToStatusDTO(transaction);
    }

    // ── Billing history ───────────────────────────────────────────────────────

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

    // ── Admin: all billing history ────────────────────────────────────────────

    public Page<PaymentHistoryDTO> getAllBillingHistoryForAdmin(Pageable pageable) {
        return transactionRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(paymentHistory -> PaymentHistoryDTO.builder()
                        .id(paymentHistory.getId())
                        .transactionId(paymentHistory.getTransactionId())
                        .companyName(paymentHistory.getCompany().getName())
                        .planType(paymentHistory.getPlanType())
                        .amount(paymentHistory.getAmount())
                        .currency(paymentHistory.getCurrency())
                        .status(paymentHistory.getStatus())
                        .createdAt(paymentHistory.getCreatedAt())
                        .paidAt(paymentHistory.getPaidAt())
                        .build()
                );
//                .map(this::mapToHistoryDTO);
    }

    // ── Cancel (user closed popup without paying) ─────────────────────────────

    @Transactional
    public void cancelTransaction(String transactionId) {
        PaymentTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TRANSACTION_NOT_FOUND", "No transaction found: " + transactionId));

        if (transaction.getStatus() == PaymentStatus.PENDING) {
            transaction.setStatus(PaymentStatus.CANCELLED);
            transactionRepository.save(transaction);
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

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