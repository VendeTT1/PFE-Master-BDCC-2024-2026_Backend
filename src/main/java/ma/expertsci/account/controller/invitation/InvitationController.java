package ma.expertsci.account.controller.invitation;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.invitation.AcceptInvitationRequestDTO;
import ma.expertsci.account.dto.invitation.InvitationRequestDTO;
import ma.expertsci.account.dto.invitation.InvitationResponseDTO;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.account.service.InvitationService;
import ma.expertsci.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

//@CrossOrigin("*")
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final UserRepository userRepository;

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/invite")
    public ResponseEntity<InvitationResponseDTO> invite(
            @RequestBody InvitationRequestDTO request,
            Authentication authentication
    ) {
        String email = authentication.getName();

        User owner = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User with email " + email + " not found"));

        Company company = owner.getCompany();

        return ResponseEntity.ok(
                invitationService.inviteStaff(request, company)
        );
    }
}