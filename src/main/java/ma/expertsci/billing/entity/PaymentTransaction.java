package ma.expertsci.billing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.subscriptions.entities.PlanType;

import java.time.LocalDateTime;

/**
 * Persists every payment attempt regardless of outcome.
 * This is the source of truth for billing — never trust webhook data alone;
 * always cross-reference with CinetPay's verify endpoint.
 */
@Entity
@Table(name = "payment_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Your own unique identifier sent to CinetPay at initiation.
     * Used to match incoming webhook callbacks to this record.
     * Must be unique per attempt — generated as UUID with no special chars.
     */
    @Column(nullable = false, unique = true)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private String currency;

    /**
     * Token returned by CinetPay after a successful initiation call.
     * Passed to the Seamless SDK on the frontend to open the payment popup.
     */
    private String cinetpayToken;

    /**
     * Raw response code from CinetPay's verify endpoint (e.g. "00" = accepted).
     * Stored for audit/debugging purposes.
     */
    private String cinetpayResponseCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /** Set once by the webhook handler; never updated again after SUCCESS. */
    private LocalDateTime paidAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}