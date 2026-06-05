package ma.expertsci.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cinetpay")
public class CinetPayConfig {

    private String apiPassword;
    private String apiKey;
    private String siteId;
    private String notifyUrl;
    private String returnUrl;
    private String paymentUrl;
    private String verifyUrl;
    private String currency;
    private String channels;
    private String lang;
}