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
import java.util.Locale;
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
            @Value("${app.bootstrap.admin.enabled:false}") boolean bootstrapAdminEnabled,
            @Value("${app.bootstrap.admin.username:}") String bootstrapAdminUsername,
            @Value("${app.bootstrap.admin.password:}") String bootstrapAdminPassword,
            @Value("${app.bootstrap.demo-accounts-enabled:false}") boolean demoAccountsEnabled) {

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

            if (bootstrapAdminEnabled) {
                ensureAdminUser(
                        userRepository,
                        adminRole,
                        bootstrapAdminUsername,
                        bootstrapAdminPassword
                );
            }

            if (demoAccountsEnabled) {
                ensureSeedUser(
                        userRepository,
                        staffRole,
                        "staff1",
                        "123456",
                        "Nhân",
                        "Viên",
                        "staff@cinema.com"
                );

                ensureSeedUser(
                        userRepository,
                        userRole,
                        "user1",
                        "123456",
                        "Khách",
                        "Hàng",
                        "user1@cinema.com"
                );
            }

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

    private void ensureAdminUser(
            UserRepository userRepository,
            Role adminRole,
            String username,
            String rawPassword) {

        if (!org.springframework.util.StringUtils.hasText(username)
                || !org.springframework.util.StringUtils.hasText(rawPassword)) {
            throw new IllegalStateException(
                    "APP_BOOTSTRAP_ADMIN_USERNAME and APP_BOOTSTRAP_ADMIN_PASSWORD are required when admin bootstrap is enabled."
            );
        }

        if (userRepository.findByUsername(username.trim()).isPresent()) {
            return;
        }

        User adminUser = User.builder()
                .username(username.trim())
                .password(passwordEncoder.encode(rawPassword))
                .roles(Set.of(adminRole))
                .emailVerified(true)
                .emailVerificationTokenHash(null)
                .emailVerificationExpiresAt(null)
                .isActive(true)
                .isDeleted(false)
                .build();

        userRepository.save(adminUser);
        log.warn("Da tao tai khoan ADMIN bootstrap: {}. Hay tat bootstrap sau khi khoi tao.", username.trim());
    }

    private void ensureSeedUser(
            UserRepository userRepository,
            Role role,
            String username,
            String rawPassword,
            String firstName,
            String lastName,
            String email) {

        String normalizedEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        var existingUser = userRepository.findByUsername(username);

        if (existingUser.isEmpty() && normalizedEmail != null && userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            log.warn("Skip seed user {} because email {} already exists.", username, normalizedEmail);
            return;
        }

        if (existingUser.isPresent()) {
            return;
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .firstName(firstName)
                .lastName(lastName)
                .email(normalizedEmail)
                .emailVerified(true)
                .emailVerificationTokenHash(null)
                .emailVerificationExpiresAt(null)
                .isActive(true)
                .isDeleted(false)
                .roles(Set.of(role))
                .build();

        userRepository.save(user);
        log.info("Created optional demo {} account: {}", role.getName(), username);
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
    }
}
