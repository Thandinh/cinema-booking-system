package com.cinema.booking.controller;

import com.cinema.booking.dto.request.UserCreationRequest;
import com.cinema.booking.dto.request.UserUpdateRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.UserResponse;
import com.cinema.booking.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller quản lý User.
 *
 * <h3>URL structure</h3>
 * <pre>
 * POST   /api/v1/users/register          — Đăng ký tài khoản (public)
 * POST   /api/v1/users                   — Admin tạo tài khoản
 * GET    /api/v1/users                   — Admin / Staff lấy danh sách user (phân trang)
 * GET    /api/v1/users/me                — User lấy profile của mình
 * GET    /api/v1/users/{id}              — Admin / Staff lấy thông tin user bất kỳ
 * PUT    /api/v1/users/{id}              — Admin cập nhật user bất kỳ (kể cả roles)
 * PATCH  /api/v1/users/me               — User tự cập nhật profile
 * DELETE /api/v1/users/{id}             — Admin xoá mềm user
 * PATCH  /api/v1/users/{id}/block       — Admin khoá tài khoản
 * PATCH  /api/v1/users/{id}/unblock     — Admin mở khoá tài khoản
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {

    UserService userService;

    // =========================================================================
    // PUBLIC – ĐĂNG KÝ
    // =========================================================================

    /**
     * Đăng ký tài khoản mới.
     * Endpoint public, không cần JWT.
     * Tự động gán role USER.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserCreationRequest request) {
        UserResponse response = userService.register(request);
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Registration successful")
                .result(response)
                .build();
    }

    // =========================================================================
    // ADMIN – QUẢN LÝ USERS
    // =========================================================================

    /**
     * Admin tạo tài khoản với tuỳ chọn roles.
     * Yêu cầu: {@code USER_CREATE} permission.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreationRequest request) {
        UserResponse response = userService.createByAdmin(request);
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("User created successfully")
                .result(response)
                .build();
    }

    /**
     * Lấy danh sách tất cả users (phân trang).
     * Yêu cầu: {@code USER_VIEW} permission.
     *
     * @param pageable ?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ApiResponse<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<UserResponse> page = userService.getAllUsers(pageable);
        return ApiResponse.<Page<UserResponse>>builder()
                .code(1000)
                .result(page)
                .build();
    }

    /**
     * Lấy thông tin user bất kỳ theo ID.
     * Yêu cầu: {@code USER_VIEW} permission.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID id) {
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .result(userService.getUserById(id))
                .build();
    }

    /**
     * Admin cập nhật user bất kỳ — bao gồm cả roles.
     * Yêu cầu: {@code USER_UPDATE} permission.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("User updated successfully")
                .result(userService.updateUser(id, request))
                .build();
    }

    /**
     * Xoá mềm user (isDeleted = true).
     * Yêu cầu: {@code USER_DELETE} permission.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ApiResponse<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("User deleted successfully")
                .build();
    }

    /**
     * Khoá tài khoản user.
     * Yêu cầu: {@code USER_BLOCK} permission.
     * Admin không thể tự block chính mình.
     */
    @PatchMapping("/{id}/block")
    @PreAuthorize("hasAuthority('USER_BLOCK')")
    public ApiResponse<UserResponse> blockUser(
            @PathVariable UUID id,
            Authentication authentication) {
        String currentUsername = authentication.getName();
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("User blocked successfully")
                .result(userService.blockUser(id, currentUsername))
                .build();
    }

    /**
     * Mở khoá tài khoản user.
     * Yêu cầu: {@code USER_BLOCK} permission.
     */
    @PatchMapping("/{id}/unblock")
    @PreAuthorize("hasAuthority('USER_BLOCK')")
    public ApiResponse<UserResponse> unblockUser(@PathVariable UUID id) {
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("User unblocked successfully")
                .result(userService.unblockUser(id))
                .build();
    }

    // =========================================================================
    // USER – TỰ QUẢN LÝ PROFILE
    // =========================================================================

    /**
     * Lấy profile của chính mình.
     * Bất kỳ user đã đăng nhập đều có quyền gọi.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile() {
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .result(userService.getMyProfile())
                .build();
    }

    /**
     * User tự cập nhật profile của mình.
     * Yêu cầu: {@code PROFILE_UPDATE} permission.
     * Roles KHÔNG được cập nhật qua endpoint này dù có kèm roleIds.
     */
    @PatchMapping("/me")
    @PreAuthorize("hasAuthority('PROFILE_UPDATE')")
    public ApiResponse<UserResponse> updateMyProfile(
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        String currentUsername = authentication.getName();
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Profile updated successfully")
                .result(userService.updateMyProfile(currentUsername, request))
                .build();
    }
}
