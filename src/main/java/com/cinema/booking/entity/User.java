package com.cinema.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String firstName;

    private String lastName;

    private LocalDate dob;

    private String phone;

    @Column(unique = true)
    private String email;

    @Column(length = 500)
    private String avatarUrl;

    @Builder.Default
    private Boolean emailVerified = true;

    @Column(length = 64)
    private String emailVerificationTokenHash;

    private LocalDateTime emailVerificationExpiresAt;

    @Column(length = 64)
    private String passwordResetTokenHash;

    private LocalDateTime passwordResetExpiresAt;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isDeleted = false;

    @Builder.Default
    private Integer authVersion = 0;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
