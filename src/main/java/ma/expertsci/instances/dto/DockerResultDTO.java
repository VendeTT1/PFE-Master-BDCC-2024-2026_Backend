package ma.expertsci.instances.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DockerResultDTO {
    private String appContainerId;
    private String dbContainerId;
    private String url;
}
