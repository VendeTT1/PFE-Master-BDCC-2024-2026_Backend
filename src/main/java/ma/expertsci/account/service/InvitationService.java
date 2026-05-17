package ma.expertsci.account.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.invitation.InvitationRequestDTO;
import ma.expertsci.account.dto.invitation.InvitationResponseDTO;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.invitation.Invitation;
import ma.expertsci.account.entities.invitation.InvitationStatus;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserRole;
import ma.expertsci.account.entities.user.UserStatus;
import ma.expertsci.account.repository.InvitationRepository;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.emailconf.service.EmailService;
import ma.expertsci.exception.BusinessRuleViolationException;
import ma.expertsci.exception.ExternalServiceException;
import ma.expertsci.exception.ResourceAlreadyExistsException;
import ma.expertsci.subscriptions.entities.PlanType;
import ma.expertsci.subscriptions.entities.Subscription;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import ma.expertsci.subscriptions.service.SubscriptionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public InvitationResponseDTO inviteStaff(InvitationRequestDTO request, Company company) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("EMAIL_ALREADY_EXISTS",
                    "User with this email already exists");
        }

        if (!canInviteStaff(company)) {
            throw new BusinessRuleViolationException("STAFF_LIMIT_REACHED",
                    "User limit reached for the trial period. Upgrade required.");
        }


        subscriptionService.checkSubscriptionValidity(company);

        String temporaryPassword = generateTemporaryPassword();

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(temporaryPassword))
                .role(UserRole.STAFF)
                .company(company)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        Invitation invitation = Invitation.builder()
                .email(request.getEmail())
                .token(UUID.randomUUID().toString())
                .status(InvitationStatus.ACCEPTED)
                .expirationDate(LocalDateTime.now().plusDays(1))
                .company(company)
                .build();

        invitationRepository.save(invitation);

        createStaffUserInOdoo(user, temporaryPassword);

        emailService.sendStaffInvite(
                request.getEmail(),
                request.getFirstName(),
                temporaryPassword
        );

        subscriptionService.activeUsersSnapshotCounter(company.getName());

        return InvitationResponseDTO.builder()
                .email(request.getEmail())
                .status("SENT")
                .expirationDate(invitation.getExpirationDate())
//                .temporaryPassword(temporaryPassword) // keep for testing, remove later
                .build();
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    public void createStaffUserInOdoo(User user, String temporaryPassword) {
        String instanceName = user.getCompany().getName();

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec",
                instanceName + "_app",
                "python3",
                "/script/create_staff_user.py",
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                temporaryPassword
        );
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[ODOO STAFF SCRIPT] " + line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new ExternalServiceException("ODOO_STAFF_CREATION_FAILED",
                        "Failed to create staff user in Odoo (exit code " + exitCode + ")", null);
            }
        } catch (IOException e) {
            throw new ExternalServiceException("ODOO_STAFF_CREATION_FAILED",
                    "I/O error while creating staff user in Odoo", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("ODOO_STAFF_CREATION_FAILED",
                    "Interrupted while creating staff user in Odoo", e);
        }
    }

    public boolean canInviteStaff(Company company) {
        Subscription subscription = subscriptionRepository.findByCompany(company)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        int activeUsers = userRepository.countByCompanyAndStatus(company, UserStatus.ACTIVE);

        // Allow only up to 5 users during trial
        if (subscription.getPlanType() == PlanType.TRIAL && activeUsers >= 5) {
            return false;  // Limit reached, block new invites
        }

        return true;  // Allow invites if under limit
    }

}