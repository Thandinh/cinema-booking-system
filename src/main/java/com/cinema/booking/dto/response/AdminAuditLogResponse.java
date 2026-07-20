package com.cinema.booking.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminAuditLogResponse {

    UUID id;
    UUID actorId;
    String actorUsername;
    String httpMethod;
    String action;
    String resource;
    String resourceId;
    String requestPath;
    String queryString;
    String ipAddress;
    String userAgent;
    Integer statusCode;
    Boolean success;
    String errorMessage;
    LocalDateTime createdAt;
}
