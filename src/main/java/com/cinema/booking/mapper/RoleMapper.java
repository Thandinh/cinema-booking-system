package com.cinema.booking.mapper;

import com.cinema.booking.dto.request.RoleRequest;
import com.cinema.booking.dto.response.RoleResponse;
import com.cinema.booking.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Mapper cho Role entity <-> DTO.
 * Phụ thuộc vào PermissionMapper để map nested permissions.
 */
@Component
@RequiredArgsConstructor
public class RoleMapper {

    private final PermissionMapper permissionMapper;

    /**
     * Chuyển RoleRequest -> Role entity (dùng khi tạo mới).
     * Lưu ý: permissions sẽ được gán trong Service sau khi fetch từ DB.
     */
    public Role toRole(RoleRequest request) {
        if (request == null) return null;
        return Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /**
     * Chuyển Role entity -> RoleResponse (bao gồm nested permissions).
     */
    public RoleResponse toRoleResponse(Role role) {
        if (role == null) return null;
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(
                        role.getPermissions() == null
                                ? Collections.emptySet()
                                : role.getPermissions().stream()
                                        .map(permissionMapper::toPermissionResponse)
                                        .collect(Collectors.toSet())
                )
                .build();
    }

    /**
     * Cập nhật Role entity từ RoleRequest (dùng khi update).
     * Chỉ cập nhật các field không null. Permissions được xử lý ở Service.
     */
    public void updateRole(Role role, RoleRequest request) {
        if (request == null) return;
        if (request.getName() != null) role.setName(request.getName());
        if (request.getDescription() != null) role.setDescription(request.getDescription());
    }
}
