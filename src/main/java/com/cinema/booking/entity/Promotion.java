package com.cinema.booking.entity;

import com.cinema.booking.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "promotions")
public class Promotion extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    @Column(nullable = false, unique = true, length = 50)
    String code;

    @Column(columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    DiscountType discountType = DiscountType.PERCENT;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal discountValue;

    @Column(precision = 10, scale = 2)
    BigDecimal maxDiscountAmount;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(nullable = false)
    LocalDateTime startDate;

    @Column(nullable = false)
    LocalDateTime endDate;

    Integer usageLimit;

    @Builder.Default
    Integer usedCount = 0;

    @Builder.Default
    Boolean isActive = true;

    @Builder.Default
    Boolean isDeleted = false;
}
