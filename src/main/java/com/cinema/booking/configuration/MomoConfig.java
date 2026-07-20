package com.cinema.booking.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "momo")
@Getter
@Setter
public class MomoConfig {
    private boolean enabled;
    private String endpoint;
    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String requestType = "captureWallet";
    private String redirectUrl;
    private String ipnUrl;
    private String lang = "vi";

    public boolean isReady() {
        return enabled
                && hasText(endpoint)
                && hasText(partnerCode)
                && hasText(accessKey)
                && hasText(secretKey)
                && hasText(requestType)
                && hasText(redirectUrl)
                && hasText(ipnUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Bean
    public RestTemplate momoRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}
