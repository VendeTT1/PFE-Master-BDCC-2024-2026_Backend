package ma.expertsci.subscriptions.controller;

import lombok.RequiredArgsConstructor;
import ma.expertsci.subscriptions.dto.SubscriptionResponseDTO;
import ma.expertsci.subscriptions.entities.Subscription;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import ma.expertsci.subscriptions.service.SubscriptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Page<SubscriptionResponseDTO>> getAllSubscriptionsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        size = Math.min(size, 50);

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                subscriptionService.getAllSubscriptionsForAdmin(pageable)
        );
    }

}