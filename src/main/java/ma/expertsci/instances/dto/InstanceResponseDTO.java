package ma.expertsci.instances.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InstanceResponseDTO {
    private Long id;
    private String name;
    private String url;
    private String status;
}
