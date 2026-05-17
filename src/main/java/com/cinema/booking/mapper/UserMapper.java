package com.cinema.booking.mapper;

import com.cinema.booking.dto.request.UserCreationRequest;
import com.cinema.booking.dto.request.UserUpdateRequest;
import com.cinema.booking.dto.response.UserResponse;
import com.cinema.booking.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Mapper cho User entity <-> DTO.
 * Phụ thuộc vào RoleMapper để map nested roles và permissions.
 */
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final RoleMapper roleMapper;

    /**
     * Chuyển UserCreateRequest -> User entity (dùng khi đăng ký / admin tạo user).
     * Lưu ý: password encoding và roles được xử lý tại Service layer.
     */
    public User toUser(UserCreationRequest request) {
        if (request == null) return null;
        return User.builder()
                .username(request.getUsername())
                .password(request.getPassword()) // raw password — Service sẽ encode
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dob(request.getDob())
                .phone(request.getPhone())
                .email(request.getEmail())
                .isActive(true)
                .isDeleted(false)
                .build();
    }

    /**
     * Chuyển User entity -> UserResponse (trả về API, không lộ password, isDeleted).
     */
    public UserResponse toUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .phone(user.getPhone())
                .email(user.getEmail())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(
                        user.getRoles() == null
                                ? Collections.emptySet()
                                : user.getRoles().stream()
                                        .map(roleMapper::toRoleResponse)
                                        .collect(Collectors.toSet())
                )
                .build();
    }

    /**
     * Cập nhật User entity từ UserUpdateRequest (dùng khi update profile / admin update).
     * Chỉ cập nhật các field không null — partial update pattern.
     * Roles và password encoding được xử lý tại Service layer.
     */
    public void updateUser(User user, UserUpdateRequest request) {
        if (request == null) return;
        if (request.getPassword() != null) user.setPassword(request.getPassword()); // Service sẽ encode
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getDob() != null) user.setDob(request.getDob());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        // roleIds được xử lý riêng trong Service để fetch entity từ DB
    }
}
