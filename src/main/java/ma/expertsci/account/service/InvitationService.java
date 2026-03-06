package ma.expertsci.account.service;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.invitation.InvitationResponseDTO;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.invitation.Invitation;
import ma.expertsci.account.entities.invitation.InvitationStatus;
import ma.expertsci.account.repository.CompanyRepository;
import ma.expertsci.account.repository.InvitationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final CompanyRepository companyRepository;

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
}
