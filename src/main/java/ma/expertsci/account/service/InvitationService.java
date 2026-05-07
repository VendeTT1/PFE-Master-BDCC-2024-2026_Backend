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
import ma.expertsci.subscriptions.service.SubscriptionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
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

    @Transactional
    public InvitationResponseDTO inviteStaff(InvitationRequestDTO request, Company company) throws Exception {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
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

    public void createStaffUserInOdoo(User user, String temporaryPassword) throws Exception {
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
            throw new RuntimeException("Failed to create staff user in Odoo");
        }
    }
}