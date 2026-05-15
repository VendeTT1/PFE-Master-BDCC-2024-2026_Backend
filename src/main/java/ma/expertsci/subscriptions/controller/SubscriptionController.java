package ma.expertsci.subscriptions.controller;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.repository.CompanyRepository;
import ma.expertsci.exception.BusinessRuleViolationException;
import ma.expertsci.exception.ResourceNotFoundException;
import ma.expertsci.subscriptions.dto.SubscriptionPlanDTO;
import ma.expertsci.subscriptions.dto.SubscriptionResponseDTO;
import ma.expertsci.subscriptions.entities.PlanType;
import ma.expertsci.subscriptions.entities.Subscription;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import ma.expertsci.subscriptions.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;
    private final CompanyRepository companyRepository;

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping
    public ResponseEntity<SubscriptionResponseDTO> getSubscription() {
        return ResponseEntity.ok(
                subscriptionService.getSubscriptionForCurrentUser()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<Page<SubscriptionResponseDTO>> getAllSubscriptionsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        size = Math.min(size, 50);

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                subscriptionService.getAllSubscriptionsForAdmin(pageable)
        );
    }

    @GetMapping("/check_status/{companyId}")
    public ResponseEntity<String> checkSubscriptionStatus(@PathVariable Long companyId) {

        // Retrieve the company from the ID
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("COMPANY_NOT_FOUND",
                        "Company with id " + companyId + " not found"));

        // Check the subscription validity. Business-rule failures map to the polled
        // "EXPIRED" status the frontend expects; anything else propagates to the
        // global handler.
        try {
            subscriptionService.checkSubscriptionValidity(company);
            return ResponseEntity.ok("ACTIVE");
        } catch (BusinessRuleViolationException e) {
            log.debug("Subscription status for company {} is invalid: [{}] {}",
                    companyId, e.getErrorCode(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("EXPIRED");
        }
    }

    // Get available plans for the user
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/plans")
    public List<SubscriptionPlanDTO> getAvailablePlans() {
        return subscriptionService.getAvailablePlans();
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/UpgradeSubscription/{companyName}/{planType}")
    public ResponseEntity<HttpStatus> UpgradeSubscription(@PathVariable String companyName,
                                                         @PathVariable PlanType planType) {
        Company company = companyRepository.findByName(companyName);
        if (company == null) {
            throw new ResourceNotFoundException("COMPANY_NOT_FOUND",
                    "Company with name " + companyName + " not found");
        }
        log.info("Upgrading subscription for company {} to plan {}", company.getName(), planType);
        subscriptionService.UpgradeSubscriptionForPlan(company, planType);
        return ResponseEntity.ok(HttpStatus.OK);
    }
}