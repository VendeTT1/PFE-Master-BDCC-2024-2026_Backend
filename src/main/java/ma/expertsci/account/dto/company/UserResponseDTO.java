package ma.expertsci.account.dto.company;


import lombok.Builder;
import lombok.Data;
import ma.expertsci.account.entities.user.UserStatus;

@Data
@Builder
public class UserResponseDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private UserStatus status;
}
