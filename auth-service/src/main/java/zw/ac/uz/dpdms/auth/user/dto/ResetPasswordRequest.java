package zw.ac.uz.dpdms.auth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import zw.ac.uz.dpdms.auth.user.PasswordPolicy;

public record ResetPasswordRequest(
        @NotBlank
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
        String newPassword) {
}
