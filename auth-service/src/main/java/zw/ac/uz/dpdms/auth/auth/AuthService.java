package zw.ac.uz.dpdms.auth.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.ac.uz.dpdms.auth.auth.dto.ChangePasswordRequest;
import zw.ac.uz.dpdms.auth.auth.dto.LoginRequest;
import zw.ac.uz.dpdms.auth.auth.dto.LoginResponse;
import zw.ac.uz.dpdms.auth.user.UserAccount;
import zw.ac.uz.dpdms.auth.user.UserAccountRepository;
import zw.ac.uz.dpdms.auth.user.dto.UserResponse;
import zw.ac.uz.dpdms.common.exception.ResourceNotFoundException;
import zw.ac.uz.dpdms.common.security.CurrentUser;
import zw.ac.uz.dpdms.common.security.JwtService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AuthService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Used so that unknown usernames take as long to reject as wrong passwords (no user enumeration by timing). */
    private final String dummyHash;

    public AuthService(UserAccountRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /** noRollbackFor: the failed-attempt counter must be saved even though we throw. */
    @Transactional(noRollbackFor = AuthenticationException.class)
    public LoginResponse login(LoginRequest request) {
        LocalDateTime now = LocalDateTime.now();
        UserAccount user = repository.findByUsernameIgnoreCase(request.username().trim()).orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        if (!user.isEnabled()) {
            throw new DisabledException("This account is disabled. Contact the provincial administrator.");
        }
        if (user.isLocked(now)) {
            throw new LockedException("Account locked after too many failed attempts. Try again after "
                    + user.getLockedUntil().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.registerFailedLogin(MAX_FAILED_ATTEMPTS, LOCK_DURATION, now);
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        user.registerSuccessfulLogin(now);
        String token = jwtService.generateToken(user.toAuthenticatedUser());
        return new LoginResponse(token, "Bearer", jwtService.getExpiration().toSeconds(), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return UserResponse.from(currentAccount());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserAccount user = currentAccount();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new IllegalArgumentException("New password must be different from the current one");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private UserAccount currentAccount() {
        String username = CurrentUser.get().username();
        return repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User " + username + " no longer exists"));
    }
}
