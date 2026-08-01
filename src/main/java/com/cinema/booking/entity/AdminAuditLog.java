package com.cinema.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    UUID actorId;

    @Column(length = 255)
    String actorUsername;

    @Column(length = 20, nullable = false)
    String httpMethod;

    @Column(length = 80, nullable = false)
    String action;

    @Column(length = 80, nullable = false)
    String resource;

    @Column(length = 100)
    String resourceId;

    @Column(length = 500, nullable = false)
    String requestPath;

    @Column(length = 500)
    String queryString;

    @Column(length = 80)
    String ipAddress;

    @Column(length = 500)
    String userAgent;

    Integer statusCode;

    Boolean success;

    @Column(length = 1000)
    String errorMessage;
}
