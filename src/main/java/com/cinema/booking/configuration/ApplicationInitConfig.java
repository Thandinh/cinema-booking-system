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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            @Value("${app.admin.default-password:admin123}") String adminDefaultPassword,
            @Value("${app.staff.default-password:123456}") String staffDefaultPassword,
            @Value("${app.user.default-password:123456}") String userDefaultPassword) {

        return args -> {
            log.info("--- BAT DAU KHOI TAO DU LIEU CINEMA RBAC ---");

            Arrays.stream(PermissionName.values()).forEach(permissionName -> {
                String name = permissionName.name();
                if (permissionRepository.findByName(name).isEmpty()) {
                    permissionRepository.save(Permission.builder()
                            .name(name)
                            .description("Quyen: " + name)
                            .build());
                }
            });
            log.info("Da khoi tao xong danh sach permissions.");

            Role userRole = upsertRole(
                    roleRepository,
                    permissionRepository,
                    RoleName.USER,
                    "Khach hang dat ve",
                    userPermissionNames()
            );

            Role staffRole = upsertRole(
                    roleRepository,
                    permissionRepository,
                    RoleName.STAFF,
                    "Nhan vien van hanh rap",
                    staffPermissionNames()
            );

            Role adminRole = roleRepository.findByName(RoleName.ADMIN.name())
                    .orElse(Role.builder()
                            .name(RoleName.ADMIN.name())
                            .description("Quan tri vien toan he thong")
                            .build());
            adminRole.setPermissions(new HashSet<>(permissionRepository.findAll()));
            adminRole = roleRepository.save(adminRole);

            ensureAdminUser(userRepository, adminRole, adminDefaultPassword);

            ensureSeedUser(
                    userRepository,
                    staffRole,
                    "staff1",
                    staffDefaultPassword,
                    "Nhân",
                    "Viên",
                    "staff@cinema.com"
            );

            ensureSeedUser(
                    userRepository,
                    userRole,
                    "user1",
                    userDefaultPassword,
                    "Khách",
                    "Hàng",
                    "user1@cinema.com"
            );

            log.info("--- KHOI TAO DU LIEU HOAN TAT ---");
        };
    }

    private Role upsertRole(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RoleName roleName,
            String description,
            Set<String> permissionNames) {

        Role role = roleRepository.findByName(roleName.name())
                .orElse(Role.builder()
                        .name(roleName.name())
                        .description(description)
                        .build());

        role.setDescription(description);
        role.setPermissions(new HashSet<>(permissionRepository.findAllByNameIn(permissionNames)));
        return roleRepository.save(role);
    }

    private void ensureAdminUser(UserRepository userRepository, Role adminRole, String adminDefaultPassword) {
        if (userRepository.findByUsername("admin").isPresent()) {
            return;
        }

        User adminUser = User.builder()
                .username("admin")
                .password(passwordEncoder.encode(adminDefaultPassword))
                .roles(Set.of(adminRole))
                .isActive(true)
                .isDeleted(false)
                .build();

        userRepository.save(adminUser);
        log.warn("Da tao tai khoan ADMIN mac dinh: admin. Hay doi password ngay.");
    }

    private void ensureSeedUser(
            UserRepository userRepository,
            Role role,
            String username,
            String rawPassword,
            String firstName,
            String lastName,
            String email) {

        var existingUser = userRepository.findByUsername(username);

        if (existingUser.isEmpty() && userRepository.findByEmail(email).isPresent()) {
            log.warn("Skip seed user {} because email {} already exists.", username, email);
            return;
        }

        User user = existingUser.orElseGet(() -> User.builder()
                .username(username)
                .roles(new HashSet<>())
                .build());

        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setIsActive(true);
        user.setIsDeleted(false);

        Set<Role> roles = user.getRoles() == null ? new HashSet<>() : new HashSet<>(user.getRoles());
        roles.add(role);
        user.setRoles(roles);

        userRepository.save(user);

        if (existingUser.isEmpty()) {
            log.info("Created default {} account: {} / {}", role.getName(), username, rawPassword);
        } else {
            log.info("Ensured default {} account is active: {}", role.getName(), username);
        }
    }

    private Set<String> userPermissionNames() {
        return Set.of(
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
    }

    private Set<String> staffPermissionNames() {
        return Set.of(
                PermissionName.MOVIE_VIEW.name(),
                PermissionName.CINEMA_VIEW.name(),
                PermissionName.ROOM_VIEW.name(),
                PermissionName.ROOM_UPDATE.name(),
                PermissionName.SEAT_VIEW.name(),
                PermissionName.SEAT_UPDATE.name(),
                PermissionName.SHOWTIME_VIEW.name(),
                PermissionName.SHOWTIME_CREATE.name(),
                PermissionName.SHOWTIME_UPDATE.name(),
                PermissionName.BOOKING_CREATE.name(),
                PermissionName.BOOKING_VIEW_OWN.name(),
                PermissionName.BOOKING_VIEW_ALL.name(),
                PermissionName.BOOKING_CANCEL_OWN.name(),
                PermissionName.BOOKING_UPDATE_STATUS.name(),
                PermissionName.PAYMENT_CREATE.name(),
                PermissionName.PAYMENT_VIEW_OWN.name(),
                PermissionName.PAYMENT_VIEW_ALL.name(),
                PermissionName.PROMOTION_VIEW.name(),
                PermissionName.TICKET_VIEW_OWN.name(),
                PermissionName.TICKET_VIEW_ALL.name(),
                PermissionName.TICKET_CHECKIN.name(),
                PermissionName.DASHBOARD_VIEW.name(),
                PermissionName.REPORT_VIEW.name(),
                PermissionName.ANALYTICS_VIEW.name()
        );
    }
}
