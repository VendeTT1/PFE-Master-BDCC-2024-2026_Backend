package ma.expertsci.account.dto.invitation;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class InvitationResponseDTO {

    private String email;

    private String status;

    private LocalDateTime expirationDate;

    private String temporaryPassword;

}
