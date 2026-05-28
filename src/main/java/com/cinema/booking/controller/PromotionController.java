package com.cinema.booking.controller;

import com.cinema.booking.dto.request.PromotionCreationRequest;
import com.cinema.booking.dto.request.PromotionUpdateRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.PromotionResponse;
import com.cinema.booking.service.PromotionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromotionController {

    PromotionService promotionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROMOTION_CREATE')")
    public ApiResponse<PromotionResponse> createPromotion(@Valid @RequestBody PromotionCreationRequest request) {
        return ApiResponse.<PromotionResponse>builder()
                .code(1000)
                .message("Promotion created successfully")
                .result(promotionService.createPromotion(request))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public ApiResponse<PromotionResponse> getPromotionById(@PathVariable UUID id) {
        return ApiResponse.<PromotionResponse>builder()
                .code(1000)
                .result(promotionService.getPromotionById(id))
                .build();
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public ApiResponse<PromotionResponse> getPromotionByCode(@PathVariable String code) {
        return ApiResponse.<PromotionResponse>builder()
                .code(1000)
                .result(promotionService.getPromotionByCode(code))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public ApiResponse<Page<PromotionResponse>> getAllPromotions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.<Page<PromotionResponse>>builder()
                .code(1000)
                .result(promotionService.getAllPromotions(pageable))
                .build();
    }

    @GetMapping("/available")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public ApiResponse<Page<PromotionResponse>> getAvailablePromotions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.<Page<PromotionResponse>>builder()
                .code(1000)
                .result(promotionService.getAvailablePromotions(pageable))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_UPDATE')")
    public ApiResponse<PromotionResponse> updatePromotion(
            @PathVariable UUID id,
            @Valid @RequestBody PromotionUpdateRequest request) {
        return ApiResponse.<PromotionResponse>builder()
                .code(1000)
                .message("Promotion updated successfully")
                .result(promotionService.updatePromotion(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_DELETE')")
    public ApiResponse<Void> deletePromotion(@PathVariable UUID id) {
        promotionService.deletePromotion(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Promotion deleted successfully")
                .build();
    }
}
