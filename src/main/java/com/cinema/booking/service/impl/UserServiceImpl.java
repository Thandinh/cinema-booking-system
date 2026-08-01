package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.ChangePasswordRequest;
import com.cinema.booking.dto.request.ForgotPasswordRequest;
import com.cinema.booking.dto.request.ResetPasswordRequest;
import com.cinema.booking.dto.request.UserCreationRequest;
import com.cinema.booking.dto.request.UserUpdateRequest;
import com.cinema.booking.dto.response.CinemaResponse;
import com.cinema.booking.dto.response.RoleResponse;
import com.cinema.booking.dto.response.UserResponse;
import com.cinema.booking.entity.Cinema;
import com.cinema.booking.entity.Role;
import com.cinema.booking.entity.StaffCinema;
import com.cinema.booking.entity.StaffCinemaId;
import com.cinema.booking.entity.User;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.RoleName;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.UserMapper;
import com.cinema.booking.repository.CinemaRepository;
import com.cinema.booking.repository.RoleRepository;
import com.cinema.booking.repository.StaffCinemaRepository;
import com.cinema.booking.repository.UserRepository;
import com.cinema.booking.service.EmailService;
import com.cinema.booking.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Triển khai UserService.
 *
 * <h3>Nguyên tắc thiết kế</h3>
 * <ul>
 *   <li>Soft-delete: xoá = set {@code isDeleted=true}, không bao giờ DELETE thật.</li>
 *   <li>Partial update: chỉ ghi đè field nếu request != null (mapper xử lý).</li>
 *   <li>Password encoding tập trung ở đây, mapper nhận raw password.</li>
 *   <li>Role resolution tập trung ở đây, mapper không chạm DB.</li>
 *   <li>@Transactional trên write ops để đảm bảo atomicity.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository   userRepository;
    RoleRepository   roleRepository;
    CinemaRepository cinemaRepository;
    StaffCinemaRepository staffCinemaRepository;
    UserMapper       userMapper;
    PasswordEncoder  passwordEncoder;
    EmailService     emailService;
    Environment      environment;

    static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // =========================================================================
    // PUBLIC / ADMIN – TẠO TÀI KHOẢN
    // =========================================================================

    /**
     * Đăng ký tài khoản mới (public endpoint).
     * Tự động gán role USER mặc định.
     */
    @Override
    @Transactional
    public UserResponse register(UserCreationRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateUniqueConstraints(request.getUsername(), normalizedEmail);

        User user = userMapper.toUser(request);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(getDefaultUserRole()));
        String rawVerificationToken = prepareEmailVerification(user);

        User saved = userRepository.save(user);
        emailService.sendEmailVerification(saved.getEmail(), saved.getUsername(), rawVerificationToken);
        log.info("Registered new user: {}", saved.getUsername());
        return userMapper.toUserResponse(saved);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationTokenHash(hashToken(token))
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_VERIFICATION_INVALID));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            clearEmailVerification(user);
            userRepository.save(user);
            return;
        }

        if (user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            clearEmailVerification(user);
            userRepository.save(user);
            throw new AppException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }

        user.setEmailVerified(true);
        clearEmailVerification(user);
        userRepository.save(user);
        log.info("Verified email for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void resendEmailVerification(String email) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> !Boolean.TRUE.equals(user.getIsDeleted()))
                .filter(user -> !Boolean.TRUE.equals(user.getEmailVerified()))
                .ifPresent(user -> {
                    String rawVerificationToken = createVerificationTokenFor(user);
                    userRepository.save(user);
                    emailService.sendEmailVerification(user.getEmail(), user.getUsername(), rawVerificationToken);
                    log.info("Resent verification email for user: {}", user.getUsername());
                });
    }

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.getEmail())
                .filter(user -> !Boolean.TRUE.equals(user.getIsDeleted()))
                .filter(user -> !Boolean.FALSE.equals(user.getIsActive()))
                .ifPresent(user -> {
                    long expiresMinutes = getPasswordResetExpiresMinutes();
                    String rawResetToken = createPasswordResetTokenFor(user, expiresMinutes);
                    userRepository.save(user);
                    emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), rawResetToken, expiresMinutes);
                    log.info("Password reset requested for user: {}", user.getUsername());
                });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetTokenHash(hashToken(request.getToken()))
                .orElseThrow(() -> new AppException(ErrorCode.PASSWORD_RESET_INVALID));

        if (user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            clearPasswordReset(user);
            userRepository.save(user);
            throw new AppException(ErrorCode.PASSWORD_RESET_INVALID);
        }

        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setEmailVerified(true);
        clearPasswordReset(user);
        clearEmailVerification(user);
        userRepository.save(user);
        log.info("Password reset completed for user: {}", user.getUsername());
    }

    /**
     * Admin tạo tài khoản với roles tuỳ chọn.
     * Nếu request không kèm roleIds → mặc định role USER.
     */
    @Override
    @Transactional
    public UserResponse createByAdmin(UserCreationRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateUniqueConstraints(request.getUsername(), normalizedEmail);

        User user = userMapper.toUser(request);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(getDefaultUserRole())); // Admin sẽ assign role qua updateUser nếu cần
        user.setEmailVerified(true);
        clearEmailVerification(user);

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            user.setRoles(resolveRoles(request.getRoleIds()));
        }
        User saved = userRepository.save(user);
        syncStaffCinemaAssignments(saved, request.getAssignedCinemaIds());
        log.info("Admin created user: {}", saved.getUsername());
        return toUserResponseWithAssignedCinemas(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .description(role.getDescription())
                        .build())
                .toList();
    }

    // =========================================================================
    // ADMIN – QUẢN LÝ USERS
    // =========================================================================

    /**
     * Danh sách tất cả user chưa bị xoá mềm, có phân trang.
     * Read-only → không cần @Transactional.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String role,
                                          String keyword,
                                          String assignedCity,
                                          UUID assignedCinemaId,
                                          boolean unassignedStaff,
                                          Pageable pageable) {
        String normalizedRole = normalizeRoleFilter(role);
        String normalizedAssignedCity = normalizeAssignedCity(assignedCity);
        if (assignedCinemaId != null && unassignedStaff) {
            throw new AppException(ErrorCode.PARAMETER_INVALID);
        }
        if (assignedCinemaId != null || normalizedAssignedCity != null || unassignedStaff) {
            if (normalizedRole != null && !RoleName.STAFF.name().equals(normalizedRole)) {
                throw new AppException(ErrorCode.PARAMETER_INVALID);
            }
            normalizedRole = RoleName.STAFF.name();
        }
        String keywordPattern = normalizeKeywordPattern(keyword);
        Page<UUID> userIdPage = userRepository.findActiveIdsByRoleKeywordAndStaffCinema(
                normalizedRole,
                keywordPattern,
                normalizedAssignedCity,
                assignedCinemaId,
                unassignedStaff,
                pageable);
        if (userIdPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, userIdPage.getTotalElements());
        }

        List<UUID> ids = userIdPage.getContent();
        Map<UUID, User> usersById = userRepository.findAllWithRolesByIdIn(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
        Map<UUID, Set<CinemaResponse>> assignedCinemasByStaffId = findAssignedCinemasByStaffIds(ids);

        List<UserResponse> content = ids.stream()
                .map(usersById::get)
                .filter(Objects::nonNull)
                .map(user -> toUserResponseWithAssignedCinemas(
                        user,
                        assignedCinemasByStaffId.getOrDefault(user.getId(), Set.of())))
                .toList();

        return new PageImpl<>(content, pageable, userIdPage.getTotalElements());
    }

    private String normalizeRoleFilter(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ADMIN", "STAFF", "USER").contains(normalized)) {
            throw new AppException(ErrorCode.PARAMETER_INVALID);
        }
        return normalized;
    }

    private String normalizeKeywordPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private String normalizeAssignedCity(String assignedCity) {
        if (assignedCity == null || assignedCity.isBlank()) {
            return null;
        }
        return assignedCity.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Lấy thông tin user bất kỳ theo ID (dùng cho ADMIN / STAFF).
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = findActiveUserById(id);
        return toUserResponseWithAssignedCinemas(user);
    }

    /**
     * Admin cập nhật user bất kỳ, bao gồm cả roles.
     *
     * <p>Logic roles:
     * <ul>
     *   <li>Nếu {@code request.roleIds} không null và không rỗng → resolve và set roles mới.</li>
     *   <li>Nếu null hoặc rỗng → giữ nguyên roles hiện tại.</li>
     * </ul>
     */
    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        User user = findActiveUserById(id);
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (normalizedEmail != null) {
            userRepository.findByEmailIgnoreCase(normalizedEmail)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(dup -> { throw new AppException(ErrorCode.EMAIL_EXISTED); });
            request.setEmail(normalizedEmail);
        }

        // Cập nhật các field cơ bản (partial update)
        userMapper.updateUser(user, request);

        // Encode password nếu có thay đổi
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Cập nhật roles nếu admin cung cấp danh sách mới
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> newRoles = resolveRoles(request.getRoleIds());
            if (user.getUsername().equals(getCurrentUsername())
                    && newRoles.stream().noneMatch(role -> RoleName.ADMIN.name().equals(role.getName()))) {
                throw new AppException(ErrorCode.CANNOT_CHANGE_OWN_ADMIN_ROLE);
            }
            user.setRoles(newRoles);
        }

        User saved = userRepository.save(user);
        if (request.getAssignedCinemaIds() != null || !hasRole(saved, RoleName.STAFF.name())) {
            syncStaffCinemaAssignments(saved, request.getAssignedCinemaIds());
        }
        log.info("Admin updated user id={}", id);
        return toUserResponseWithAssignedCinemas(saved);
    }

    /**
     * Xoá mềm user: set {@code isDeleted=true}.
     * Không bao giờ DELETE thật khỏi DB.
     */
    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = findActiveUserById(id);
        if (user.getUsername().equals(getCurrentUsername())) {
            throw new AppException(ErrorCode.CANNOT_DELETE_SELF);
        }
        user.setIsDeleted(true);
        userRepository.save(user);
        log.info("Soft-deleted user id={}", id);
    }

    /**
     * Khoá tài khoản (isActive = false).
     * Chặn admin tự block chính mình.
     */
    @Override
    @Transactional
    public UserResponse blockUser(UUID targetId, String currentUsername) {
        User target = findActiveUserById(targetId);

        if (target.getUsername().equals(currentUsername)) {
            throw new AppException(ErrorCode.CANNOT_BLOCK_SELF);
        }

        target.setIsActive(false);
        User saved = userRepository.save(target);
        log.warn("Blocked user: {}", target.getUsername());
        return userMapper.toUserResponse(saved);
    }

    /**
     * Mở khoá tài khoản (isActive = true).
     */
    @Override
    @Transactional
    public UserResponse unblockUser(UUID targetId) {
        User target = findActiveUserById(targetId);
        target.setIsActive(true);
        User saved = userRepository.save(target);
        log.info("Unblocked user: {}", target.getUsername());
        return userMapper.toUserResponse(saved);
    }

    @Override
    @Transactional
    public void requestPasswordResetByAdmin(UUID targetId) {
        User target = findActiveUserById(targetId);
        String email = normalizeEmail(target.getEmail());
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.EMAIL_REQUIRED);
        }

        long expiresMinutes = getPasswordResetExpiresMinutes();
        String rawResetToken = createPasswordResetTokenFor(target, expiresMinutes);
        userRepository.save(target);
        emailService.sendPasswordResetEmail(email, target.getUsername(), rawResetToken, expiresMinutes);
        log.info("Admin requested password reset email for user id={}", targetId);
    }

    // =========================================================================
    // USER – TỰ QUẢN LÝ PROFILE
    // =========================================================================

    /**
     * Lấy profile của chính mình từ SecurityContext.
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return toUserResponseWithAssignedCinemas(user);
    }

    /**
     * User tự cập nhật profile cá nhân.
     * KHÔNG cập nhật roles dù request có kèm roleIds (bị bỏ qua).
     */
    @Override
    @Transactional
    public UserResponse updateMyProfile(String currentUsername, UserUpdateRequest request) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        String normalizedEmail = normalizeEmail(request.getEmail());

        // Kiểm tra email mới không trùng user khác
        if (normalizedEmail != null) {
            userRepository.findByEmailIgnoreCase(normalizedEmail)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(dup -> { throw new AppException(ErrorCode.EMAIL_EXISTED); });
            request.setEmail(normalizedEmail);
        }

        // Partial update (mapper bỏ qua null fields)
        userMapper.updateUser(user, request);

        // roleIds bị bỏ qua hoàn toàn — PROFILE_UPDATE không cho phép đổi role
        User saved = userRepository.save(user);
        log.info("User {} updated own profile", currentUsername);
        return toUserResponseWithAssignedCinemas(saved);
    }

    @Override
    @Transactional
    public void changeMyPassword(String currentUsername, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INVALID);
        }

        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("User {} changed password", currentUsername);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /** Lấy username từ SecurityContext hiện tại. */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getName();
    }

    /**
     * Tìm user đang active (chưa bị xoá mềm).
     * Ném USER_NOT_FOUND nếu không tồn tại.
     */
    private User findActiveUserById(UUID id) {
        return userRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Validate username và email chưa tồn tại trong hệ thống.
     */
    private void validateUniqueConstraints(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolve danh sách UUID sang Role entities.
     * Ném ROLE_NOT_FOUND nếu bất kỳ ID nào không tồn tại.
     */
    private Set<Role> resolveRoles(Set<UUID> roleIds) {
        Set<Role> roles = new HashSet<>();
        for (UUID roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            roles.add(role);
        }
        return Collections.unmodifiableSet(roles);
    }

    private void syncStaffCinemaAssignments(User staff, Set<UUID> assignedCinemaIds) {
        if (!hasRole(staff, RoleName.STAFF.name())) {
            staffCinemaRepository.deleteByStaffId(staff.getId());
            return;
        }
        if (assignedCinemaIds == null) {
            return;
        }

        staffCinemaRepository.deleteByStaffId(staff.getId());
        if (assignedCinemaIds.isEmpty()) {
            return;
        }

        List<StaffCinema> assignments = assignedCinemaIds.stream()
                .distinct()
                .map(cinemaId -> {
                    Cinema cinema = cinemaRepository.findActiveById(cinemaId)
                            .orElseThrow(() -> new AppException(ErrorCode.CINEMA_NOT_FOUND));
                    return StaffCinema.builder()
                            .id(StaffCinemaId.builder()
                                    .staffId(staff.getId())
                                    .cinemaId(cinema.getId())
                                    .build())
                            .staff(staff)
                            .cinema(cinema)
                            .build();
                })
                .toList();
        staffCinemaRepository.saveAll(assignments);
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getName()));
    }

    private UserResponse toUserResponseWithAssignedCinemas(User user) {
        Set<CinemaResponse> assignedCinemas = staffCinemaRepository.findAllWithCinemaByStaffIds(List.of(user.getId())).stream()
                .map(StaffCinema::getCinema)
                .map(this::toCinemaResponse)
                .collect(Collectors.toSet());
        return toUserResponseWithAssignedCinemas(user, assignedCinemas);
    }

    private UserResponse toUserResponseWithAssignedCinemas(User user, Set<CinemaResponse> assignedCinemas) {
        UserResponse response = userMapper.toUserResponse(user);
        response.setAssignedCinemas(assignedCinemas);
        return response;
    }

    private Map<UUID, Set<CinemaResponse>> findAssignedCinemasByStaffIds(List<UUID> staffIds) {
        if (staffIds == null || staffIds.isEmpty()) {
            return Map.of();
        }
        return staffCinemaRepository.findAllWithCinemaByStaffIds(staffIds).stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getStaff().getId(),
                        Collectors.mapping(
                                assignment -> toCinemaResponse(assignment.getCinema()),
                                Collectors.toSet())));
    }

    private CinemaResponse toCinemaResponse(Cinema cinema) {
        return CinemaResponse.builder()
                .id(cinema.getId())
                .name(cinema.getName())
                .address(cinema.getAddress())
                .city(cinema.getCity())
                .latitude(cinema.getLatitude())
                .longitude(cinema.getLongitude())
                .isActive(cinema.getIsActive())
                .createdAt(cinema.getCreatedAt())
                .updatedAt(cinema.getUpdatedAt())
                .build();
    }

    /**
     * Lấy role USER mặc định khi đăng ký.
     * Ném ROLE_NOT_FOUND nếu DB chưa được seed (ApplicationInitConfig chưa chạy).
     */
    private Role getDefaultUserRole() {
        return roleRepository.findByName(RoleName.USER.name())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }

    private String prepareEmailVerification(User user) {
        user.setEmailVerified(false);
        return createVerificationTokenFor(user);
    }

    private String createVerificationTokenFor(User user) {
        String rawToken = createRawToken();
        user.setEmailVerificationTokenHash(hashToken(rawToken));
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(getVerificationExpiresMinutes()));
        return rawToken;
    }

    private String createPasswordResetTokenFor(User user, long expiresMinutes) {
        String rawToken = createRawToken();
        user.setPasswordResetTokenHash(hashToken(rawToken));
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(expiresMinutes));
        return rawToken;
    }

    private String createRawToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void clearEmailVerification(User user) {
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
    }

    private void clearPasswordReset(User user) {
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
    }

    private long getVerificationExpiresMinutes() {
        return environment.getProperty("app.email-verification.expires-minutes", Long.class, 1440L);
    }

    private long getPasswordResetExpiresMinutes() {
        return environment.getProperty("app.password-reset.expires-minutes", Long.class, 30L);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash verification token", e);
        }
    }
}
