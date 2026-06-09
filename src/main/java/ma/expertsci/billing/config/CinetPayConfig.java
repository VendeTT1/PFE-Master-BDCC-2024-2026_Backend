package ma.expertsci.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cinetpay")
public class CinetPayConfig {
    private String apiKey;
    private String apiPassword;
    private String country;
    private String baseUrl;
    private String notifyUrl;
    private String returnUrl;
    private String successUrl;
    private String failedUrl;
    private String currency;
    private String channels;
    private String lang;
}