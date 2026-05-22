package com.cinema.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    Cinema cinema;

    @Column(nullable = false)
    String name;

    @Builder.Default
    Boolean isDeleted = false;
}
