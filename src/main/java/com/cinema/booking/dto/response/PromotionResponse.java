package com.cinema.booking.dto.response;

import com.cinema.booking.enums.DiscountType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionResponse {
    UUID id;
    String code;
    String description;
    DiscountType discountType;
    BigDecimal discountValue;
    BigDecimal maxDiscountAmount;
    BigDecimal minOrderValue;
    LocalDateTime startDate;
    LocalDateTime endDate;
    Integer usageLimit;
    Integer usedCount;
    Boolean isActive;
}
