package ma.expertsci.account.dto.user;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String userRole;
    private String status;
    private String email;
}
