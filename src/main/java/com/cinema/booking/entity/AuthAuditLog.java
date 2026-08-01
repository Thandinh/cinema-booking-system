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
@Table(name = "auth_audit_logs")
public class AuthAuditLog extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    UUID userId;

    @Column(length = 255)
    String username;

    @Column(length = 80, nullable = false)
    String eventType;

    @Column(nullable = false)
    Boolean success;

    @Column(length = 1000)
    String failureReason;

    @Column(length = 80)
    String ipAddress;

    @Column(length = 500)
    String userAgent;
}
