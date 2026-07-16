package com.cinema.booking.security.config;

import com.cinema.booking.security.jwt.CustomJwtDecoder;
import com.cinema.booking.security.jwt.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomJwtDecoder customJwtDecoder;

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/auth/token",
            "/auth/google",
            "/auth/introspect",
            "/auth/logout",
            "/auth/refresh",
            "/api/v1/users/register",
            "/api/v1/users/verify-email",
            "/api/v1/users/resend-verification"
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/v1/payments/vnpay-callback",
            "/api/v1/movies/**",
            "/api/v1/showtimes/**",
            "/api/v1/bookings/showtimes/**",
            "/api/v1/cinemas",
            "/api/v1/cinemas/**",
            "/api/v1/cinemas/map",      // Leaflet: lấy tất cả rạp có tọa độ
            "/api/v1/cinemas/nearest"   // Leaflet: tìm rạp gần nhất (Haversine)
    };

    // WebSocket handshake + SockJS fallback endpoints phải được permit
    // (dữ liệu push là public: trạng thái ghế ai cũng được xem)
    private static final String[] PUBLIC_WS_ENDPOINTS = {
            "/ws/**",           // SockJS endpoint và tất cả sub-paths
            "/ws/info/**",      // SockJS info endpoint
            "/ws-native/**"     // Native WebSocket endpoint (không cần SockJS)
    };

    // Các endpoint của Swagger/OpenAPI để xem tài liệu API
    private static final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(request -> request
                .requestMatchers(PUBLIC_WS_ENDPOINTS).permitAll()
                .requestMatchers(SWAGGER_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                .anyRequest().authenticated());

        httpSecurity.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .decoder(customJwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));

        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix(""); 

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
