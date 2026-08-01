package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.PromotionCreationRequest;
import com.cinema.booking.dto.request.PromotionUpdateRequest;
import com.cinema.booking.dto.response.PromotionResponse;
import com.cinema.booking.configuration.CacheConfig;
import com.cinema.booking.entity.Promotion;
import com.cinema.booking.enums.DiscountType;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.PromotionAdminStatus;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.PromotionMapper;
import com.cinema.booking.repository.PromotionRepository;
import com.cinema.booking.service.PromotionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    PromotionRepository promotionRepository;
    PromotionMapper promotionMapper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PROMOTIONS, allEntries = true)
    public PromotionResponse createPromotion(PromotionCreationRequest request) {
        // 1. Kiểm tra ngày tháng hợp lệ
        if (request.getEndDate().isBefore(request.getStartDate()) || request.getEndDate().isEqual(request.getStartDate())) {
            throw new AppException(ErrorCode.PROMOTION_END_DATE_INVALID);
        }

        // --- TỰ ĐỘNG SINH MÃ NẾU ĐỂ TRỐNG ---
        String finalCode;
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            finalCode = generateRandomPromoCode(8);
        } else {
            finalCode = request.getCode().trim().toUpperCase();
        }
        request.setCode(finalCode);

        // 2. Kiểm tra trùng lặp mã
        if (promotionRepository.existsByCodeAndIsDeletedFalse(request.getCode())) {
            throw new AppException(ErrorCode.PROMOTION_CODE_EXISTS);
        }

        // 3. Chặn lỗi nhập liệu % giảm giá
        if (request.getDiscountType() == DiscountType.PERCENT 
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.DISCOUNT_VALUE_INVALID);
        }

        // 4. Xóa maxDiscountAmount nếu là giảm tiền mặt (FIXED) để tránh nhiễu dữ liệu
        if (request.getDiscountType() == DiscountType.FIXED) {
            request.setMaxDiscountAmount(null);
        }

        Promotion promotion = promotionMapper.toPromotion(request);
        Promotion saved = promotionRepository.save(promotion);
        log.info("Created promotion with code: {}", saved.getCode());
        return promotionMapper.toPromotionResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PROMOTIONS, allEntries = true)
    public PromotionResponse updatePromotion(UUID id, PromotionUpdateRequest request) {
        Promotion promotion = promotionRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        LocalDateTime newStartDate = request.getStartDate() != null ? request.getStartDate() : promotion.getStartDate();
        LocalDateTime newEndDate = request.getEndDate() != null ? request.getEndDate() : promotion.getEndDate();

        if (newEndDate.isBefore(newStartDate) || newEndDate.isEqual(newStartDate)) {
            throw new AppException(ErrorCode.PROMOTION_END_DATE_INVALID);
        }

        // --- XỬ LÝ LOGIC NGHIỆP VỤ KHI UPDATE MỘT PHẦN ---
        // Lấy giá trị thực tế sẽ được lưu (kết hợp giữa request mới và data cũ)
        DiscountType effectiveType = request.getDiscountType() != null ? request.getDiscountType() : promotion.getDiscountType();
        BigDecimal effectiveValue = request.getDiscountValue() != null ? request.getDiscountValue() : promotion.getDiscountValue();

        // Kiểm tra chặn giảm quá 100%
        if (effectiveType == DiscountType.PERCENT && effectiveValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.DISCOUNT_VALUE_INVALID);
        }

        // Nếu admin đổi hình thức từ PERCENT sang FIXED, tự động dọn dẹp trường MaxDiscount
        if (effectiveType == DiscountType.FIXED) {
            request.setMaxDiscountAmount(null);
            promotion.setMaxDiscountAmount(null); // Ép xóa trực tiếp trên Entity cũ
        }

        promotionMapper.updatePromotion(promotion, request);
        Promotion saved = promotionRepository.save(promotion);
        log.info("Updated promotion id: {}", saved.getId());
        return promotionMapper.toPromotionResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PROMOTIONS, allEntries = true)
    public void deletePromotion(UUID id) {
        Promotion promotion = promotionRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
        
        promotion.setIsDeleted(true);
        promotionRepository.save(promotion);
        log.info("Soft-deleted promotion id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(UUID id) {
        return promotionRepository.findActiveById(id)
                .map(promotionMapper::toPromotionResponse)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionByCode(String code) {
        return promotionRepository.findActiveByCode(code.trim().toUpperCase())
                .map(promotionMapper::toPromotionResponse)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionResponse> getAllPromotions(Pageable pageable) {
        return promotionRepository.findAllActive(pageable)
                .map(promotionMapper::toPromotionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionResponse> getAdminPromotions(
            Pageable pageable,
            PromotionAdminStatus status,
            String keyword) {
        PromotionAdminStatus effectiveStatus = status == null ? PromotionAdminStatus.ALL : status;
        String keywordPattern = keyword == null || keyword.isBlank()
                ? null
                : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

        Boolean active = effectiveStatus == PromotionAdminStatus.INACTIVE ? false : null;
        boolean availableOnly = effectiveStatus == PromotionAdminStatus.AVAILABLE;
        boolean upcomingOnly = effectiveStatus == PromotionAdminStatus.UPCOMING;
        boolean expiredOnly = effectiveStatus == PromotionAdminStatus.EXPIRED;
        boolean exhaustedOnly = effectiveStatus == PromotionAdminStatus.EXHAUSTED;

        return promotionRepository.searchAdminPromotions(
                        keywordPattern,
                        active,
                        availableOnly,
                        upcomingOnly,
                        expiredOnly,
                        exhaustedOnly,
                        LocalDateTime.now(),
                        pageable)
                .map(promotionMapper::toPromotionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheConfig.PROMOTIONS,
            key = "'available:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort"
    )
    public Page<PromotionResponse> getAvailablePromotions(Pageable pageable) {
        return promotionRepository.findAvailablePromotions(LocalDateTime.now(), pageable)
                .map(promotionMapper::toPromotionResponse);
    }

    private String generateRandomPromoCode(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        while (result.length() < length) {
            int index = (int) (rnd.nextFloat() * characters.length());
            result.append(characters.charAt(index));
        }
        return result.toString();
    }
}
