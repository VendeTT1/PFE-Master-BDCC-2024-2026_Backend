package ma.expertsci.subscriptions.service;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.subscriptions.dto.SubscriptionResponseDTO;
import ma.expertsci.subscriptions.entities.PlanType;
import ma.expertsci.subscriptions.entities.Subscription;
import ma.expertsci.subscriptions.entities.SubscriptionStatus;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

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

    // This checks if the subscription is active or expired
    public boolean isSubscriptionActive(Company company) {

        Subscription subscription = subscriptionRepository
                .findByCompany(company)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (subscription.getEndDate().isBefore(LocalDateTime.now())) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            return false;  // Subscription is expired
        }

        return subscription.getStatus() == SubscriptionStatus.ACTIVE;  // Active subscription
    }

    // This method will enforce that the company can use the service
    public void checkSubscriptionValidity(Company company) {

        Subscription subscription = subscriptionRepository
                .findByCompany(company)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        LocalDateTime now = LocalDateTime.now();

        // Check if the subscription is suspended or expired
        if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
            throw new RuntimeException("Subscription is suspended");
        }

        if (subscription.getStatus() == SubscriptionStatus.EXPIRED ||
                (subscription.getEndDate() != null && subscription.getEndDate().isBefore(now))) {
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
                .planType(subscription.getPlanType().name())
                .status(subscription.getStatus().name())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
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
}