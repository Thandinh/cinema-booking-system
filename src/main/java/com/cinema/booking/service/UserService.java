package com.cinema.booking.service;

import com.cinema.booking.dto.request.UserCreationRequest;
import com.cinema.booking.dto.request.UserUpdateRequest;
import com.cinema.booking.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Contract cho mọi thao tác quản lý User.
 * <p>
 * Phân tách rõ:
 * <ul>
 *   <li>Admin operations  – getAll, updateAny, delete, block/unblock, assignRoles</li>
 *   <li>User operations   – getMyProfile, updateMyProfile</li>
 * </ul>
 */
public interface UserService {

    // ─── Public / Admin: tạo tài khoản ───────────────────────────────────────

    /**
     * Đăng ký tài khoản mới (public endpoint, mặc định role USER).
     * Yêu cầu: username & email chưa tồn tại.
     */
    UserResponse register(UserCreationRequest request);

    /**
     * Admin tạo tài khoản với roles tuỳ chỉnh.
     * Nếu request.roleIds rỗng → gán role USER mặc định.
     */
    UserResponse createByAdmin(UserCreationRequest request);

    // ─── Admin: quản lý users ────────────────────────────────────────────────

    /** Danh sách tất cả user chưa bị xoá mềm, có phân trang. */
    Page<UserResponse> getAllUsers(Pageable pageable);

    /** Lấy thông tin bất kỳ user theo ID (ADMIN / STAFF). */
    UserResponse getUserById(UUID id);

    /**
     * Admin cập nhật thông tin user bất kỳ.
     * Nếu request.roleIds != null → cập nhật roles.
     */
    UserResponse updateUser(UUID id, UserUpdateRequest request);

    /** Xoá mềm user (isDeleted = true). */
    void deleteUser(UUID id);

    /**
     * Khoá / mở khoá tài khoản.
     *
     * @param targetId  UUID của user cần tác động
     * @param currentUsername username của người đang thực hiện (lấy từ JWT)
     *                  — dùng để chặn admin tự block chính mình
     */
    UserResponse blockUser(UUID targetId, String currentUsername);

    UserResponse unblockUser(UUID targetId);

    // ─── User: tự quản lý profile ─────────────────────────────────────────────

    /**
     * Lấy profile của chính mình (lấy username từ SecurityContext).
     */
    UserResponse getMyProfile();

    /**
     * Người dùng tự cập nhật profile (password, firstName, lastName, dob, phone, email).
     * KHÔNG được phép cập nhật roles qua endpoint này.
     *
     * @param currentUsername username lấy từ JWT
     */
    UserResponse updateMyProfile(String currentUsername, UserUpdateRequest request);
}
