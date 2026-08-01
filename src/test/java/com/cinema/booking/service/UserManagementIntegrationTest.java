package com.cinema.booking.service;

import com.cinema.booking.dto.request.UserUpdateRequest;
import com.cinema.booking.dto.response.UserResponse;
import com.cinema.booking.entity.Cinema;
import com.cinema.booking.entity.Role;
import com.cinema.booking.entity.StaffCinema;
import com.cinema.booking.entity.StaffCinemaId;
import com.cinema.booking.entity.User;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.RoleName;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.repository.CinemaRepository;
import com.cinema.booking.repository.RoleRepository;
import com.cinema.booking.repository.StaffCinemaRepository;
import com.cinema.booking.repository.UserRepository;
import com.cinema.booking.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "booking.scheduling.enabled=false",
        "ticket.qr-secret=test-ticket-qr-secret-32-characters-minimum"
})
class UserManagementIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    CinemaRepository cinemaRepository;

    @Autowired
    StaffCinemaRepository staffCinemaRepository;

    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsers_shouldFilterByRoleAndKeywordWithStablePagination() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Role userRole = roleRepository.findByName(RoleName.USER.name()).orElseThrow();
        Role staffRole = roleRepository.findByName(RoleName.STAFF.name()).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ADMIN.name()).orElseThrow();

        User user = userRepository.save(testUser("rolefilter-user-" + suffix, userRole, "user-" + suffix + "@test.local"));
        User staff = userRepository.save(testUser("rolefilter-staff-" + suffix, staffRole, "staff-" + suffix + "@test.local"));
        User unassignedStaff = userRepository.save(testUser("rolefilter-unassigned-staff-" + suffix, staffRole, "unassigned-staff-" + suffix + "@test.local"));
        User admin = userRepository.save(testUser("rolefilter-admin-" + suffix, adminRole, "admin-" + suffix + "@test.local"));
        Cinema assignedCinema = cinemaRepository.save(testCinema("Role Filter Cinema " + suffix, "Da Nang"));
        staffCinemaRepository.save(StaffCinema.builder()
                .id(StaffCinemaId.builder()
                        .staffId(staff.getId())
                        .cinemaId(assignedCinema.getId())
                        .build())
                .staff(staff)
                .cinema(assignedCinema)
                .build());

        Page<UserResponse> all = userService.getAllUsers(null, suffix, null, null, false, PageRequest.of(0, 10));
        Page<UserResponse> users = userService.getAllUsers("USER", suffix, null, null, false, PageRequest.of(0, 10));
        Page<UserResponse> staffUsers = userService.getAllUsers("STAFF", suffix, null, null, false, PageRequest.of(0, 10));
        Page<UserResponse> admins = userService.getAllUsers("ADMIN", suffix, null, null, false, PageRequest.of(0, 10));

        assertThat(all.getContent()).extracting(UserResponse::getId)
                .contains(user.getId(), staff.getId(), unassignedStaff.getId(), admin.getId());
        assertThat(users.getContent()).extracting(UserResponse::getId)
                .containsExactly(user.getId());
        assertThat(staffUsers.getContent()).extracting(UserResponse::getId)
                .containsExactlyInAnyOrder(staff.getId(), unassignedStaff.getId());
        assertThat(admins.getContent()).extracting(UserResponse::getId)
                .containsExactly(admin.getId());

        Page<UserResponse> staffWithoutKeyword = userService.getAllUsers("STAFF", "", null, null, false, PageRequest.of(0, 10));
        assertThat(staffWithoutKeyword.getContent()).extracting(UserResponse::getId)
                .contains(staff.getId());

        Page<UserResponse> staffByCity = userService.getAllUsers("STAFF", suffix, "Da Nang", null, false, PageRequest.of(0, 10));
        assertThat(staffByCity.getContent()).extracting(UserResponse::getId)
                .containsExactly(staff.getId());

        Page<UserResponse> staffByCinema = userService.getAllUsers("STAFF", suffix, null, assignedCinema.getId(), false, PageRequest.of(0, 10));
        assertThat(staffByCinema.getContent()).extracting(UserResponse::getId)
                .containsExactly(staff.getId());

        Page<UserResponse> staffWithoutCinema = userService.getAllUsers("STAFF", suffix, null, null, true, PageRequest.of(0, 10));
        assertThat(staffWithoutCinema.getContent()).extracting(UserResponse::getId)
                .containsExactly(unassignedStaff.getId());
    }

    @Test
    void deleteUser_shouldRejectDeletingCurrentAdminAccount() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Role adminRole = roleRepository.findByName(RoleName.ADMIN.name()).orElseThrow();
        User admin = userRepository.save(testUser("self-delete-admin-" + suffix, adminRole, "self-delete-" + suffix + "@test.local"));
        authenticateAs(admin.getUsername());

        assertThatThrownBy(() -> userService.deleteUser(admin.getId()))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_DELETE_SELF);
    }

    @Test
    void updateUser_shouldRejectRemovingCurrentAdminRoleFromSelf() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Role adminRole = roleRepository.findByName(RoleName.ADMIN.name()).orElseThrow();
        Role userRole = roleRepository.findByName(RoleName.USER.name()).orElseThrow();
        User admin = userRepository.save(testUser("self-role-admin-" + suffix, adminRole, "self-role-" + suffix + "@test.local"));
        authenticateAs(admin.getUsername());

        UserUpdateRequest request = UserUpdateRequest.builder()
                .roleIds(Set.of(userRole.getId()))
                .build();

        assertThatThrownBy(() -> userService.updateUser(admin.getId(), request))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_CHANGE_OWN_ADMIN_ROLE);
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "test", List.of()));
    }

    private User testUser(String username, Role role, String email) {
        return User.builder()
                .username(username)
                .password("{noop}123456")
                .email(email)
                .firstName("Role")
                .lastName("Filter")
                .roles(Set.of(role))
                .emailVerified(true)
                .isActive(true)
                .isDeleted(false)
                .build();
    }

    private Cinema testCinema(String name, String city) {
        return Cinema.builder()
                .name(name)
                .address("Test address")
                .city(city)
                .isActive(true)
                .isDeleted(false)
                .build();
    }
}
