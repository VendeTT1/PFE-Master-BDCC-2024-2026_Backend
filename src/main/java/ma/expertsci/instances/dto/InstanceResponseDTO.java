package ma.expertsci.instances.dto;

import lombok.Builder;
import lombok.Data;
import ma.expertsci.instances.entities.InstanceStatus;

@Builder
@Data
public class InstanceResponseDTO {
    private Long id;
    private String name;
    private String url;
    private InstanceStatus status;
}
