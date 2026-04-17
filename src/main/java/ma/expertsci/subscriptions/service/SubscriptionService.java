package ma.expertsci.subscriptions.service;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserRole;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.subscriptions.dto.SubscriptionResponseDTO;
import ma.expertsci.subscriptions.entities.PlanType;
import ma.expertsci.subscriptions.entities.Subscription;
import ma.expertsci.subscriptions.entities.SubscriptionStatus;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public Subscription initializeTrial(Company company) {

        Subscription subscription = new Subscription();
        subscription.setCompany(company);
        subscription.setPlanType(PlanType.TRIAL);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusDays(14));

        return subscriptionRepository.save(subscription);
    }

    public boolean isSubscriptionActive(Company company) {

        Subscription subscription = subscriptionRepository
                .findByCompany(company)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (subscription.getEndDate().isBefore(LocalDateTime.now())) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            return false;
        }

        return subscription.getStatus() == SubscriptionStatus.ACTIVE;
    }

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

    public List<SubscriptionResponseDTO> getAllSubscriptionsForAdmin() {
        // Fetch all subscriptions
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {
            throw new RuntimeException("Subscriptions not found");
        }
        // Map to DTOs
        List<SubscriptionResponseDTO> subscriptionDTOs = subscriptions.stream()
                .map(subscription -> SubscriptionResponseDTO.builder()
                        .planType(subscription.getPlanType().name())
                        .status(subscription.getStatus().name())
                        .startDate(subscription.getStartDate())
                        .endDate(subscription.getEndDate())
                        .companyName(subscription.getCompany().getName())
                        .userEmail(subscription.getCompany().getUsers().get(0).getEmail())
                        .build())
                .collect(Collectors.toList());

        return subscriptionDTOs;
    }

}