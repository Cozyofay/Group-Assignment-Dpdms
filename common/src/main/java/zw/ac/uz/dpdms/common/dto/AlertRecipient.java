package zw.ac.uz.dpdms.common.dto;

import zw.ac.uz.dpdms.common.domain.Role;

/** A person who should receive an alert, returned by auth-service to alert-service. */
public record AlertRecipient(String username, String fullName, Role role, String email, String phoneNumber) {
}
