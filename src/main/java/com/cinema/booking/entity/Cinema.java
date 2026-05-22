package com.cinema.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "cinemas")
public class Cinema extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    @Column(nullable = false)
    String name;

    @Column(columnDefinition = "TEXT")
    String address;

    String city;

    Double latitude;

    Double longitude;

    @Builder.Default
    Boolean isActive = true;

    @Builder.Default
    Boolean isDeleted = false;

    @OneToMany(mappedBy = "cinema", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @Builder.Default
    Set<Room> rooms = new HashSet<>();
}
