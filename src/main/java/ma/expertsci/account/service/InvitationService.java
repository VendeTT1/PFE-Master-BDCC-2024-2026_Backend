package ma.expertsci.account.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public InvitationResponseDTO inviteStaff(InvitationRequestDTO request, Company company) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

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

        return InvitationResponseDTO.builder()
                .email(request.getEmail())
                .status("SENT")
                .expirationDate(invitation.getExpirationDate())
                .temporaryPassword(temporaryPassword)
                .build();
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}