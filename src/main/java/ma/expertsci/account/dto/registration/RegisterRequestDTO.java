package ma.expertsci.account.dto.registration;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ma.expertsci.account.entities.UserRole;

@Data
public class RegisterRequestDTO {

    @NotBlank
    private String companyName;

    @NotBlank
    private String country;

    @Email
    @NotBlank
    private String email;

    @Size(min = 6)
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;
//
//    @NotBlank
//    private UserRole userRole;
}
