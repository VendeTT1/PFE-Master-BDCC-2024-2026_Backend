package ma.expertsci.billing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.expertsci.billing.dto.*;
import ma.expertsci.billing.service.BillingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    /**
     * POST /api/billing/initiate
     *
     * Initiates a payment session for the authenticated company owner.
     * Returns a paymentToken for the Seamless SDK and a transactionId for polling.
     *
     * Access: OWNER only (staff cannot trigger purchases)
     */
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<InitiatePaymentResponseDTO> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequestDTO request) {
        return ResponseEntity.ok(billingService.initiatePayment(request));
    }

    /**
     * GET /api/billing/status/{transactionId}
     *
     * Polled by the frontend after the Seamless popup closes.
     * Returns current payment status so the UI can react (show success/failure).
     *
     * Access: OWNER or STAFF of the same company
     */
    @GetMapping("/status/{transactionId}")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<PaymentStatusResponseDTO> getPaymentStatus(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(billingService.getPaymentStatus(transactionId));
    }

    /**
     * POST /api/billing/cancel/{transactionId}
     *
     * Called by the frontend when the user closes the popup without paying.
     * Marks the transaction CANCELLED so it doesn't stay PENDING forever.
     */
    @PostMapping("/cancel/{transactionId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> cancelPayment(@PathVariable String transactionId) {
        billingService.cancelTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/billing/history
     *
     * Returns the payment history for the authenticated user's company.
     * Useful for a "Billing" page in the dashboard.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<PaymentHistoryDTO>> getBillingHistory() {
        return ResponseEntity.ok(billingService.getBillingHistory());
    }

    /**
     * GET /api/admin/billing/history?page=0&size=20
     *
     * Admin view of all payment transactions across all companies.
     * Access: ADMIN only
     */
    @GetMapping("/admin/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentHistoryDTO>> getAllBillingHistory(Pageable pageable) {
        return ResponseEntity.ok(billingService.getAllBillingHistoryForAdmin(pageable));
    }
}