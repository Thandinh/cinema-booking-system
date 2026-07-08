package com.cinema.booking.configuration;

import com.cinema.booking.entity.Permission;
import com.cinema.booking.entity.Role;
import com.cinema.booking.entity.User;
import com.cinema.booking.enums.PermissionName;
import com.cinema.booking.enums.RoleName;
import com.cinema.booking.repository.PermissionRepository;
import com.cinema.booking.repository.RoleRepository;
import com.cinema.booking.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @Bean
    @ConditionalOnProperty(
            prefix = "spring",
            value = "datasource.driver-class-name",
            havingValue = "org.postgresql.Driver")
    ApplicationRunner applicationRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            @Value("${app.admin.default-password:admin123}") String adminDefaultPassword) {

        return args -> {


            log.info("--- BẮT ĐẦU KHỞI TẠO DỮ LIỆU CINEMA (RBAC) ---");

            // 1. Khởi tạo toàn bộ PERMISSIONS từ Enum
            // Duyệt qua tất cả giá trị trong PermissionName Enum
            Arrays.stream(PermissionName.values()).forEach(permissionName -> {
                String name = permissionName.name();
                if (permissionRepository.findByName(name).isEmpty()) {
                    permissionRepository.save(Permission.builder()
                            .name(name)
                            .description("Quyền: " + name)
                            .build());
                }
            });
            log.info("Đã khởi tạo xong danh sách Permissions.");

            // 2. Khởi tạo ROLES và gán Permission tương ứng

            // --- ROLE: USER (Khách hàng) ---
            if (roleRepository.findByName(RoleName.USER.name()).isEmpty()) {
                Set<String> userPermNames = Set.of(
                        PermissionName.MOVIE_VIEW.name(),
                        PermissionName.CINEMA_VIEW.name(),
                        PermissionName.SHOWTIME_VIEW.name(),
                        PermissionName.SEAT_VIEW.name(),
                        PermissionName.BOOKING_CREATE.name(),
                        PermissionName.BOOKING_VIEW_OWN.name(),
                        PermissionName.BOOKING_CANCEL_OWN.name(),
                        PermissionName.PAYMENT_CREATE.name(),
                        PermissionName.PAYMENT_VIEW_OWN.name(),
                        PermissionName.PROMOTION_VIEW.name(),
                        PermissionName.PROFILE_UPDATE.name(),
                        PermissionName.TICKET_VIEW_OWN.name(),
                        PermissionName.AUTH_LOGIN.name(),
                        PermissionName.AUTH_LOGOUT.name(),
                        PermissionName.AUTH_REFRESH_TOKEN.name()
                );
                Set<Permission> userPermissions = new HashSet<>(permissionRepository.findAllByNameIn(userPermNames));
                roleRepository.save(Role.builder()
                        .name(RoleName.USER.name())
                        .description("Khách hàng đặt vé")
                        .permissions(userPermissions)
                        .build());
            }

            // --- ROLE: STAFF (Nhân viên) ---
            if (roleRepository.findByName(RoleName.STAFF.name()).isEmpty()) {
                Set<String> staffPermNames = Set.of(
                        PermissionName.MOVIE_VIEW.name(),
                        PermissionName.CINEMA_VIEW.name(),
                        PermissionName.ROOM_VIEW.name(),
                        PermissionName.ROOM_UPDATE.name(),
                        PermissionName.SEAT_VIEW.name(),
                        PermissionName.SEAT_UPDATE.name(),
                        PermissionName.SHOWTIME_VIEW.name(),
                        PermissionName.SHOWTIME_CREATE.name(),
                        PermissionName.SHOWTIME_UPDATE.name(),
                        PermissionName.BOOKING_VIEW_ALL.name(),
                        PermissionName.BOOKING_UPDATE_STATUS.name(),
                        PermissionName.PAYMENT_VIEW_ALL.name(),
                        PermissionName.PROMOTION_VIEW.name(),
                        PermissionName.TICKET_VIEW_ALL.name(),
                        PermissionName.TICKET_CHECKIN.name(),
                        PermissionName.DASHBOARD_VIEW.name(),
                        PermissionName.REPORT_VIEW.name(),
                        PermissionName.ANALYTICS_VIEW.name()
                );
                Set<Permission> staffPermissions = new HashSet<>(permissionRepository.findAllByNameIn(staffPermNames));
                roleRepository.save(Role.builder()
                        .name(RoleName.STAFF.name())
                        .description("Nhân viên vận hành rạp")
                        .permissions(staffPermissions)
                        .build());
            }

            // --- ROLE: ADMIN (Quản trị viên) ---
            // Luôn cập nhật permissions cho ADMIN để đảm bảo có đủ quyền mới
            Role adminRoleObj = roleRepository.findByName(RoleName.ADMIN.name())
                    .orElse(Role.builder()
                            .name(RoleName.ADMIN.name())
                            .description("Quản trị viên toàn hệ thống")
                            .build());
            adminRoleObj.setPermissions(new HashSet<>(permissionRepository.findAll()));
            roleRepository.save(adminRoleObj);

            // 3. Khởi tạo tài khoản ADMIN mặc định
            if (userRepository.findByUsername("admin").isEmpty()) {
                Role adminRole = roleRepository.findByName(RoleName.ADMIN.name())
                        .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy Role ADMIN"));

                User adminUser = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode(adminDefaultPassword))
                        .roles(Set.of(adminRole))
                        .isActive(true)
                        .build();

                userRepository.save(adminUser);
                log.warn("Đã tạo tài khoản ADMIN mặc định: admin. Hãy đổi password ngay!");
            }

            log.info("--- KHỞI TẠO DỮ LIỆU HOÀN TẤT ---");
        };
    }
}
