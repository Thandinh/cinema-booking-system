package com.cinema.booking.service;

import com.cinema.booking.dto.request.PromotionCreationRequest;
import com.cinema.booking.dto.request.PromotionUpdateRequest;
import com.cinema.booking.dto.response.PromotionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PromotionService {

    PromotionResponse createPromotion(PromotionCreationRequest request);

    PromotionResponse updatePromotion(UUID id, PromotionUpdateRequest request);

    void deletePromotion(UUID id);

    PromotionResponse getPromotionById(UUID id);

    PromotionResponse getPromotionByCode(String code);

    Page<PromotionResponse> getAllPromotions(Pageable pageable);

    Page<PromotionResponse> getAvailablePromotions(Pageable pageable);
}
