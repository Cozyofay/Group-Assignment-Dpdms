package zw.ac.uz.dpdms.auth.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;
import zw.ac.uz.dpdms.common.dto.ValidationPatterns;

/** Username cannot change (audit trails reference it). Passwords change via reset-password. */
public record UpdateUserRequest(
        @NotBlank
        @Size(max = 150)
        @Pattern(regexp = ValidationPatterns.SAFE_TEXT, message = ValidationPatterns.SAFE_TEXT_MESSAGE)
        String fullName,

        @Email
        @Size(max = 150)
        String email,

        @Pattern(regexp = "^$|^\\+[1-9]\\d{7,14}$", message = "must be in international format, e.g. +263771234567")
        String phoneNumber,

        @NotNull
        Role role,

        HazardType hazard,

        @Size(max = 100)
        @Pattern(regexp = ValidationPatterns.SAFE_TEXT, message = ValidationPatterns.SAFE_TEXT_MESSAGE)
        String ward,

        @NotNull
        Boolean enabled,

        @NotNull
        Boolean receiveAlerts) {
}
