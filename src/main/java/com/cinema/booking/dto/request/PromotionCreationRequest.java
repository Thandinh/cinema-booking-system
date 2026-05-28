package com.cinema.booking.dto.request;

import com.cinema.booking.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionCreationRequest {

    String code; // Tự động sinh nếu để trống

    String description;

    @NotNull(message = "DISCOUNT_TYPE_REQUIRED")
    DiscountType discountType;

    @NotNull(message = "DISCOUNT_VALUE_REQUIRED")
    @DecimalMin(value = "0", message = "DISCOUNT_VALUE_INVALID")
    BigDecimal discountValue;

    @DecimalMin(value = "0", message = "MAX_DISCOUNT_INVALID")
    BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0", message = "MIN_ORDER_INVALID")
    BigDecimal minOrderValue;

    @NotNull(message = "START_DATE_REQUIRED")
    LocalDateTime startDate;

    @NotNull(message = "END_DATE_REQUIRED")
    @Future(message = "END_DATE_FUTURE")
    LocalDateTime endDate;

    Integer usageLimit;
}
