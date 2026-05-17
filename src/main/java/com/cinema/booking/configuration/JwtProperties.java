package com.cinema.booking.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String signerKey;

    private long accessTokenValidDuration;

    private long refreshTokenValidDuration;
}
