package zw.ac.uz.dpdms.auth.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import zw.ac.uz.dpdms.auth.user.dto.CreateUserRequest;
import zw.ac.uz.dpdms.auth.user.dto.ResetPasswordRequest;
import zw.ac.uz.dpdms.auth.user.dto.UpdateUserRequest;
import zw.ac.uz.dpdms.auth.user.dto.UserResponse;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;
import zw.ac.uz.dpdms.common.dto.AlertRecipient;
import zw.ac.uz.dpdms.common.exception.ResourceNotFoundException;
import zw.ac.uz.dpdms.common.security.AuthenticatedUser;
import zw.ac.uz.dpdms.common.security.CurrentUser;
import zw.ac.uz.dpdms.common.security.HazardAccessPolicy;

import java.util.List;

/** User management. Every operation here is restricted to the PROVINCIAL_ADMIN, checked in the backend. */
@Service
@Transactional
public class UserService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(Role role, HazardType hazard) {
        requireAdmin();
        return repository.findAllByOrderByRoleAscUsernameAsc().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> hazard == null || u.getHazard() == hazard)
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        requireAdmin();
        return UserResponse.from(find(id));
    }

    public UserResponse create(CreateUserRequest request) {
        requireAdmin();
        String username = request.username().trim();
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username '" + username + "' is already taken");
        }
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setEmail(blankToNull(request.email()));
        user.setPhoneNumber(blankToNull(request.phoneNumber()));
        user.applyScope(request.role(), request.hazard(), blankToNull(request.ward()));
        user.setReceiveAlerts(request.receiveAlerts() == null || request.receiveAlerts());
        return UserResponse.from(repository.save(user));
    }

    public UserResponse update(Long id, UpdateUserRequest request) {
        AuthenticatedUser admin = requireAdmin();
        UserAccount user = find(id);
        if (user.getUsername().equals(admin.username())
                && (request.role() != Role.PROVINCIAL_ADMIN || !request.enabled())) {
            throw new IllegalArgumentException("You cannot remove your own admin role or disable your own account");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(blankToNull(request.email()));
        user.setPhoneNumber(blankToNull(request.phoneNumber()));
        user.applyScope(request.role(), request.hazard(), blankToNull(request.ward()));
        user.setEnabled(request.enabled());
        user.setReceiveAlerts(request.receiveAlerts());
        return UserResponse.from(user);
    }

    public void delete(Long id) {
        AuthenticatedUser admin = requireAdmin();
        UserAccount user = find(id);
        if (user.getUsername().equals(admin.username())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        repository.delete(user);
    }

    public void resetPassword(Long id, ResetPasswordRequest request) {
        requireAdmin();
        UserAccount user = find(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.unlock();
    }

    /** Used by alert-service (internal call) to find who to notify about an incident. */
    @Transactional(readOnly = true)
    public List<AlertRecipient> alertRecipients(HazardType hazard, String ward) {
        return repository.findByEnabledTrueAndReceiveAlertsTrue().stream()
                .filter(u -> u.isAlertRecipientFor(hazard, ward))
                .map(u -> new AlertRecipient(u.getUsername(), u.getFullName(), u.getRole(),
                        u.getEmail(), u.getPhoneNumber()))
                .toList();
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser current = CurrentUser.get();
        HazardAccessPolicy.require(current.role() == Role.PROVINCIAL_ADMIN,
                "Only the provincial administrator can manage users");
        return current;
    }

    private UserAccount find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
