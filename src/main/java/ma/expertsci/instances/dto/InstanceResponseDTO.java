package ma.expertsci.instances.dto;

import lombok.Builder;
import lombok.Data;
import ma.expertsci.instances.entities.InstanceStatus;

@Builder
@Data
public class InstanceResponseDTO {
    private Long id;
    private String nameInstance;
    private String url;
    private String region;
    private InstanceStatus status;
    private String userEmail;
    private String firstName;
    private String lastName;
}
