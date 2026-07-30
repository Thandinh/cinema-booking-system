package com.cinema.booking.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sepay")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SePayConfig {

    boolean enabled;
    String bankCode;
    String accountNumber;
    String accountName;
    String descriptionPrefix = "CBK";
    String qrBaseUrl = "https://vietqr.app/img";
    String webhookApiKey;
    String webhookHmacSecret;

    public boolean isReady() {
        return enabled
                && hasText(bankCode)
                && hasText(accountNumber);
    }

    public boolean hasWebhookApiKey() {
        return hasText(webhookApiKey);
    }

    public boolean hasWebhookHmacSecret() {
        return hasText(webhookHmacSecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
