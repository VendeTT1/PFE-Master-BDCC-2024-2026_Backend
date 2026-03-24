package ma.expertsci.account.dto.company;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyResponseDTO {
    private Long id;
    private String name;
    private String region;
    private String ownerEmail;
    private int usersCount;
}
