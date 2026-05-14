package ma.expertsci.subscriptions.service;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserRole;
import ma.expertsci.account.repository.CompanyRepository;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.account.service.CompanyService;
import ma.expertsci.subscriptions.dto.SubscriptionPlanDTO;
import ma.expertsci.subscriptions.dto.SubscriptionResponseDTO;
import ma.expertsci.subscriptions.entities.PlanType;
import ma.expertsci.subscriptions.entities.Subscription;
import ma.expertsci.subscriptions.entities.SubscriptionStatus;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
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

    // Initialize a trial subscription for a company
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

    // This method will enforce that the company can use the service
    public void checkSubscriptionValidity(Company company) {

        Subscription subscription = subscriptionRepository
                .findByCompany(company)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        LocalDateTime now = LocalDateTime.now();

        // Check if subscription is expired
        if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
            throw new RuntimeException("Subscription is suspended");
        }

        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            if (subscription.getEndDate().isAfter(now)) {
                // Reactivate the subscription
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscriptionRepository.save(subscription);
            } else {
                throw new RuntimeException("Subscription has expired");
            }
        }

        // Check if subscription is active
        if (subscription.getEndDate() != null && subscription.getEndDate().isBefore(now)) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            throw new RuntimeException("Subscription has expired");
        }

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("Subscription is not active");
        }
    }

    // Retrieve subscription info for the current user (based on logged-in user)
    public SubscriptionResponseDTO getSubscriptionForCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getCompany() == null) {
            throw new RuntimeException("User has no company");
        }

        Subscription subscription = subscriptionRepository
                .findByCompany(user.getCompany())
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        return SubscriptionResponseDTO.builder()
                .companyName(subscription.getCompany().getName())
                .planType(subscription.getPlanType().name())
                .status(subscription.getStatus().name())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .userEmail(user.getEmail())
                .build();
    }

    // List all subscriptions for the admin user
    public Page<SubscriptionResponseDTO> getAllSubscriptionsForAdmin(Pageable pageable) {

        Page<Subscription> subscriptionsPage = subscriptionRepository.findAll(pageable);

        return subscriptionsPage.map(subscription -> {

            // Extract the user from the company
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

    public List<SubscriptionPlanDTO> getAvailablePlans() {
        return Arrays.stream(PlanType.values())  // PlanType.values() gives you all the enum values
                .map(plan -> SubscriptionPlanDTO.builder()
                        .code(plan.name())
                        .label(formatPlanLabel(plan))
                        .includedUsers(plan.getIncludedUsers())
                        .paid(plan.isPaid())
                        .build())
                .collect(Collectors.toList());
    }

    private String formatPlanLabel(PlanType planType) {
        // This method returns the label (e.g., "Trial" for TRIAL, "Premium" for PREMIUM)
        switch (planType) {
            case TRIAL:
                return "Trial";
            case PREMIUM:
                return "Premium";
            case ENTERPRISE:
                return "Enterprise";
            default:
                throw new IllegalArgumentException("Unknown plan type: " + planType);
        }
    }

    public Subscription UpgradeSubscriptionForPlan(Company company, PlanType selectedPlan) {
        Subscription subscription = subscriptionRepository
                .findByCompany(company)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        if (subscription == null) {
            Subscription newSub = new Subscription();
            newSub.setCompany(company);
            newSub.setPlanType(selectedPlan);
            newSub.setStatus(SubscriptionStatus.ACTIVE);  // Waiting for payment
            newSub.setStartDate(LocalDateTime.now());
            newSub.setEndDate(LocalDateTime.now().plusDays(selectedPlan.getDurationInDays()));  // Duration
            newSub.setIncludedUsers(selectedPlan.getIncludedUsers());
            newSub.setActiveUsersSnapshot(1);
            return subscriptionRepository.save(newSub);
        }
        else {

            subscription.setPlanType(selectedPlan);
            subscription.setStatus(SubscriptionStatus.ACTIVE);  // Waiting for payment
            subscription.setStartDate(LocalDateTime.now());
            subscription.setEndDate(LocalDateTime.now().plusDays(selectedPlan.getDurationInDays()));  // Duration
            subscription.setIncludedUsers(selectedPlan.getIncludedUsers());
            subscription.setActiveUsersSnapshot(1);// always one since the owner is an active user in the instance //TO DO : remember to increment this value after each invitation accepted

            return subscriptionRepository.save(subscription);
        }
    }

    public void activeUsersSnapshotCounter(String company) {

        Company cp = companyRepository.findByName(company);

        Subscription sub = subscriptionRepository.findByCompany(cp).orElseThrow();

        sub.setActiveUsersSnapshot(sub.getActiveUsersSnapshot() + 1);
        System.out.println("counter incremented, new number of users is : " + sub.getActiveUsersSnapshot());
    }

}