package ma.expertsci.account.dto.invitation;

import lombok.Data;

@Data
public class AcceptInvitationRequestDTO {

    private String token;

    private String password;

    private String firstName;

    private String lastName;

}