package zw.ac.uz.dpdms.auth.user.dto;

import zw.ac.uz.dpdms.auth.user.UserAccount;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;

import java.time.LocalDateTime;

/** What the API returns about a user. The password hash is never included. */
public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phoneNumber,
        Role role,
        HazardType hazard,
        String ward,
        boolean enabled,
        boolean receiveAlerts,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getPhoneNumber(), user.getRole(), user.getHazard(), user.getWard(), user.isEnabled(),
                user.isReceiveAlerts(), user.getLastLoginAt(), user.getCreatedAt());
    }
}
