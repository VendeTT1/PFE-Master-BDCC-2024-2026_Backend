package ma.expertsci.subscriptions.controller;

import lombok.RequiredArgsConstructor;
import ma.expertsci.subscriptions.dto.SubscriptionResponseDTO;
import ma.expertsci.subscriptions.entities.Subscription;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import ma.expertsci.subscriptions.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping
    public ResponseEntity<SubscriptionResponseDTO> getSubscription() {
        return ResponseEntity.ok(
                subscriptionService.getSubscriptionForCurrentUser()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<SubscriptionResponseDTO>> getAllSubscriptions() {
        List<SubscriptionResponseDTO> subscriptions = subscriptionService.getAllSubscriptionsForAdmin();
        return ResponseEntity.ok(subscriptions);
    }

}