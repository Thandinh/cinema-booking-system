package com.cinema.booking.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "oauth.google")
public class GoogleOAuthProperties {
    private String clientId;
}
