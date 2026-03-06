package ma.expertsci.account.controller.invitation;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.invitation.InvitationRequestDTO;
import ma.expertsci.account.dto.invitation.InvitationResponseDTO;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.account.service.InvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final UserRepository userRepository;

    @PostMapping("/invite")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<InvitationResponseDTO> invite(
            @RequestBody InvitationRequestDTO request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User owner = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Company company = owner.getCompany();

        return ResponseEntity.ok(
                invitationService.inviteStaff(request.getEmail(), company)
        );
    }
}