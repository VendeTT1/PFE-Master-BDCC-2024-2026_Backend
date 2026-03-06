package ma.expertsci.account.service;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.invitation.AcceptInvitationRequestDTO;
import ma.expertsci.account.dto.invitation.InvitationResponseDTO;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.invitation.Invitation;
import ma.expertsci.account.entities.invitation.InvitationStatus;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserRole;
import ma.expertsci.account.entities.user.UserStatus;
import ma.expertsci.account.repository.CompanyRepository;
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
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public InvitationResponseDTO inviteStaff(String email, Company company) {

        String token = UUID.randomUUID().toString();

        Invitation invitation = Invitation.builder()
                .email(email)
                .token(token)
                .status(InvitationStatus.PENDING)
                .expirationDate(LocalDateTime.now().plusDays(1))
                .company(company)
                .build();

        invitationRepository.save(invitation);

        return InvitationResponseDTO.builder()
                .email(email)
                .status("PENDING")
                .expirationDate(invitation.getExpirationDate())
                .build();
    }

    public void acceptInvitation(AcceptInvitationRequestDTO request) {

        Invitation invitation = invitationRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if(invitation.getStatus() != InvitationStatus.PENDING) {
            throw new RuntimeException("Invitation already used");
        }

        if(invitation.getExpirationDate().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new RuntimeException("Invitation expired");
        }

        User user = User.builder()
                .email(invitation.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.STAFF)
                .company(invitation.getCompany())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);
    }

}
