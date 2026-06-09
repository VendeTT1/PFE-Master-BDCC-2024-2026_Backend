package ma.expertsci.billing.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinetPayAuthResponseDTO {
    @JsonProperty("code")
    private int code;

    @JsonProperty("status")
    private String status;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("user_email")
    private String userEmail;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private int expiresIn;

    public boolean isSuccess() {
        return accessToken != null && !accessToken.isBlank();
    }
}
