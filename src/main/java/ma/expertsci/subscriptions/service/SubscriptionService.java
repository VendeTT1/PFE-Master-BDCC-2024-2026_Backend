package ma.expertsci.subscriptions.service;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.repository.CompanyRepository;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.exception.BusinessRuleViolationException;
import ma.expertsci.exception.ResourceNotFoundException;
import ma.expertsci.subscriptions.dto.SubscriptionPlanDTO;
import ma.expertsci.subscriptions.dto.SubscriptionResponseDTO;
import ma.expertsci.subscriptions.entities.PlanType;
import ma.expertsci.subscriptions.entities.Subscription;
import ma.expertsci.subscriptions.entities.SubscriptionStatus;
import ma.expertsci.subscriptions.exception.SubscriptionErrorCodes;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    // ── Initialize a trial subscription for a new company ────────────────────

    public Subscription initializeTrial(Company company) {

        Subscription subscription = new Subscription();
        subscription.setCompany(company);
        subscription.setPlanType(PlanType.TRIAL);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusMinutes(5)); // trial expires in 5 minutes for testing
        subscription.setIncludedUsers(5);
        subscription.setExtraUsers(0);
        subscription.setActiveUsersSnapshot(1);

        return subscriptionRepository.save(subscription);
    }

    // ── Enforce that the company may use the service ─────────────────────────

    public void checkSubscriptionValidity(Company company) {

        Subscription subscription = subscriptionRepository
                .findByCompany(company)
                .orElseThrow(() -> new ResourceNotFoundException(
                        SubscriptionErrorCodes.SUBSCRIPTION_NOT_FOUND,
                        "No subscription found for company: " + company.getName()));

        LocalDateTime now = LocalDateTime.now();

        if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
            throw new BusinessRuleViolationException(
                    SubscriptionErrorCodes.SUBSCRIPTION_SUSPENDED,
                    "Subscription for company '" + company.getName() + "' is currently suspended.");
        }

        // If marked EXPIRED but end-date is still in the future, reactivate
        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            if (subscription.getEndDate().isAfter(now)) {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscriptionRepository.save(subscription);
            } else {
                throw new BusinessRuleViolationException(
                        SubscriptionErrorCodes.SUBSCRIPTION_EXPIRED,
                        "Subscription for company '" + company.getName() + "' has expired.");
            }
        }

        // Detect expiry on a still-ACTIVE subscription
        if (subscription.getEndDate() != null && subscription.getEndDate().isBefore(now)) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            throw new BusinessRuleViolationException(
                    SubscriptionErrorCodes.SUBSCRIPTION_EXPIRED,
                    "Subscription for company '" + company.getName() + "' has expired.");
        }

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    SubscriptionErrorCodes.SUBSCRIPTION_INACTIVE,
                    "Subscription for company '" + company.getName() + "' is not active.");
        }
    }

    // ── Retrieve subscription info for the authenticated user ────────────────

    public SubscriptionResponseDTO getSubscriptionForCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        SubscriptionErrorCodes.USER_NOT_FOUND,
                        "No user found with email: " + email));

        if (user.getCompany() == null) {
            throw new ResourceNotFoundException(
                    SubscriptionErrorCodes.COMPANY_NOT_FOUND,
                    "User '" + email + "' is not associated with any company.");
        }

        Subscription subscription = subscriptionRepository
                .findByCompany(user.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException(
                        SubscriptionErrorCodes.SUBSCRIPTION_NOT_FOUND,
                        "No subscription found for company: " + user.getCompany().getName()));

        return SubscriptionResponseDTO.builder()
                .companyName(subscription.getCompany().getName())
                .planType(subscription.getPlanType().name())
                .status(subscription.getStatus().name())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .userEmail(user.getEmail())
                .build();
    }

    // ── List all subscriptions (admin) ───────────────────────────────────────

    public Page<SubscriptionResponseDTO> getAllSubscriptionsForAdmin(Pageable pageable) {

        Page<Subscription> subscriptionsPage = subscriptionRepository.findAll(pageable);

        return subscriptionsPage.map(subscription -> {

            User user = subscription.getCompany().getUsers().stream()
                    .findFirst()
                    .orElse(null);

            return SubscriptionResponseDTO.builder()
                    .planType(subscription.getPlanType().name())
                    .status(subscription.getStatus().name())
                    .startDate(subscription.getStartDate())
                    .endDate(subscription.getEndDate())
                    .companyName(subscription.getCompany().getName())
                    .userEmail(user != null ? user.getEmail() : "")
                    .build();
        });
    }

    // ── Available plans catalog ──────────────────────────────────────────────

    public List<SubscriptionPlanDTO> getAvailablePlans() {
        return Arrays.stream(PlanType.values())
                .map(plan -> SubscriptionPlanDTO.builder()
                        .code(plan.name())
                        .label(formatPlanLabel(plan))
                        .includedUsers(plan.getIncludedUsers())
                        .paid(plan.isPaid())
                        .build())
                .collect(Collectors.toList());
    }

    private String formatPlanLabel(PlanType planType) {
        switch (planType) {
            case TRIAL:      return "Trial";
            case PREMIUM:    return "Premium";
            case ENTERPRISE: return "Enterprise";
            default:
                throw new BusinessRuleViolationException(
                        SubscriptionErrorCodes.UNKNOWN_PLAN_TYPE,
                        "Unknown plan type: " + planType);
        }
    }

    // ── Upgrade / create subscription for a plan ─────────────────────────────

    public Subscription UpgradeSubscriptionForPlan(Company company, PlanType selectedPlan) {

        Subscription subscription = subscriptionRepository
                .findByCompany(company)
                .orElse(null);

        if (subscription == null) {
            Subscription newSub = new Subscription();
            newSub.setCompany(company);
            newSub.setPlanType(selectedPlan);
            newSub.setStatus(SubscriptionStatus.ACTIVE);
            newSub.setStartDate(LocalDateTime.now());
            newSub.setEndDate(LocalDateTime.now().plusDays(selectedPlan.getDurationInDays()));
            newSub.setIncludedUsers(selectedPlan.getIncludedUsers());
            newSub.setActiveUsersSnapshot(1);
            return subscriptionRepository.save(newSub);
        }

        subscription.setPlanType(selectedPlan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusDays(selectedPlan.getDurationInDays()));
        subscription.setIncludedUsers(selectedPlan.getIncludedUsers());
        subscription.setActiveUsersSnapshot(1);
        return subscriptionRepository.save(subscription);
    }

    // ── Active-users counter ─────────────────────────────────────────────────

    public void activeUsersSnapshotCounter(String companyName) {

        Company company = companyRepository.findByName(companyName);
        if (company == null) {
            throw new ResourceNotFoundException(
                    SubscriptionErrorCodes.COMPANY_NOT_FOUND,
                    "No company found with name: " + companyName);
        }

        Subscription subscription = subscriptionRepository
                .findByCompany(company)
                .orElseThrow(() -> new ResourceNotFoundException(
                        SubscriptionErrorCodes.SUBSCRIPTION_NOT_FOUND,
                        "No subscription found for company: " + companyName));

        subscription.setActiveUsersSnapshot(subscription.getActiveUsersSnapshot() + 1);
        subscriptionRepository.save(subscription);
    }
}