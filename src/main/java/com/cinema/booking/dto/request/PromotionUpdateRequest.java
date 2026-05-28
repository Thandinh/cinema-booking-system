package com.cinema.booking.dto.request;

import com.cinema.booking.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionUpdateRequest {
    
    String description;
    
    DiscountType discountType;

    @DecimalMin(value = "0", message = "DISCOUNT_VALUE_INVALID")
    BigDecimal discountValue;

    @DecimalMin(value = "0", message = "MAX_DISCOUNT_INVALID")
    BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0", message = "MIN_ORDER_INVALID")
    BigDecimal minOrderValue;

    LocalDateTime startDate;

    @Future(message = "END_DATE_FUTURE")
    LocalDateTime endDate;

    Integer usageLimit;

    Boolean isActive;
}
