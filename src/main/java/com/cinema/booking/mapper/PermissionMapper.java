package com.cinema.booking.mapper;

import com.cinema.booking.dto.request.PermissionRequest;
import com.cinema.booking.dto.response.PermissionResponse;
import com.cinema.booking.entity.Permission;
import org.springframework.stereotype.Component;

/**
 * Mapper cho Permission entity <-> DTO.
 * Sử dụng manual mapping để tránh dependency MapStruct,
 * đảm bảo tương thích với Lombok annotation processor hiện tại.
 */
@Component
public class PermissionMapper {

    /**
     * Chuyển PermissionRequest -> Permission entity (dùng khi tạo mới).
     */
    public Permission toPermission(PermissionRequest request) {
        if (request == null) return null;
        return Permission.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /**
     * Chuyển Permission entity -> PermissionResponse (dùng khi trả về API).
     */
    public PermissionResponse toPermissionResponse(Permission permission) {
        if (permission == null) return null;
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }

    /**
     * Cập nhật Permission entity từ PermissionRequest (dùng khi update).
     * Chỉ cập nhật các field không null.
     */
    public void updatePermission(Permission permission, PermissionRequest request) {
        if (request == null) return;
        if (request.getName() != null) permission.setName(request.getName());
        if (request.getDescription() != null) permission.setDescription(request.getDescription());
    }
}
