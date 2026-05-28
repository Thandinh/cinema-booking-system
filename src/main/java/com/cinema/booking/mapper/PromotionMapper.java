package com.cinema.booking.mapper;

import com.cinema.booking.dto.request.PromotionCreationRequest;
import com.cinema.booking.dto.request.PromotionUpdateRequest;
import com.cinema.booking.dto.response.PromotionResponse;
import com.cinema.booking.entity.Promotion;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {

    public Promotion toPromotion(PromotionCreationRequest request) {
        return Promotion.builder()
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : java.math.BigDecimal.ZERO)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .isActive(true)
                .isDeleted(false)
                .build();
    }

    public PromotionResponse toPromotionResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .description(promotion.getDescription())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .minOrderValue(promotion.getMinOrderValue())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .usageLimit(promotion.getUsageLimit())
                .usedCount(promotion.getUsedCount())
                .isActive(promotion.getIsActive())
                .build();
    }

    public void updatePromotion(Promotion promotion, PromotionUpdateRequest request) {
        if (request.getDescription() != null) promotion.setDescription(request.getDescription());
        if (request.getDiscountType() != null) promotion.setDiscountType(request.getDiscountType());
        if (request.getDiscountValue() != null) promotion.setDiscountValue(request.getDiscountValue());
        if (request.getMaxDiscountAmount() != null) promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getMinOrderValue() != null) promotion.setMinOrderValue(request.getMinOrderValue());
        if (request.getStartDate() != null) promotion.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) promotion.setEndDate(request.getEndDate());
        if (request.getUsageLimit() != null) promotion.setUsageLimit(request.getUsageLimit());
        if (request.getIsActive() != null) promotion.setIsActive(request.getIsActive());
    }
}
