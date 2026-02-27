package ma.expertsci.account.dto.registration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponseDTO {

    private Long companyId;
    private Long userId;
    private String message;
}
