package ma.expertsci.account.dto.login;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDTO {

    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String message;
}
