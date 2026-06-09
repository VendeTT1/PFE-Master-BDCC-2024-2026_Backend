package ma.expertsci.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CinetPayAuthRequestDTO {
    @JsonProperty("api_key")
    private String apiKey;

    @JsonProperty("api_password")
    private String apiPassword;
}
