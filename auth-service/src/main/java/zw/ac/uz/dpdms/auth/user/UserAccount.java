package zw.ac.uz.dpdms.auth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;
import zw.ac.uz.dpdms.common.security.AuthenticatedUser;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 150)
    private String email;

    /** WhatsApp number in international format, e.g. +263771234567. */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private HazardType hazard;

    @Column(length = 100)
    private String ward;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "receive_alerts", nullable = false)
    private boolean receiveAlerts = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Sets role, hazard and ward together, reusing the AuthenticatedUser rules:
     * a recorder must have a hazard and a ward, a supervisor must have a hazard, admin and
     * national users have neither (any hazard/ward sent for them is discarded).
     * Throws IllegalArgumentException (-> 400) if the combination is invalid.
     */
    public void applyScope(Role newRole, HazardType newHazard, String newWard) {
        AuthenticatedUser validated = new AuthenticatedUser(username, newRole, newHazard,
                newWard == null ? null : newWard.trim());
        this.role = validated.role();
        this.hazard = validated.hazard();
        this.ward = validated.ward();
    }

    public AuthenticatedUser toAuthenticatedUser() {
        return new AuthenticatedUser(username, role, hazard, ward);
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** Brute-force protection: after maxAttempts failures the account is locked for lockDuration. */
    public void registerFailedLogin(int maxAttempts, Duration lockDuration, LocalDateTime now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
            failedLoginAttempts = 0;
        }
    }

    public void unlock() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public void registerSuccessfulLogin(LocalDateTime now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    /**
     * Who is alerted about an incident of this hazard in this ward: that hazard's supervisor,
     * that hazard's recorders in that ward, the provincial admin and national users.
     */
    public boolean isAlertRecipientFor(HazardType incidentHazard, String incidentWard) {
        if (!enabled || !receiveAlerts) {
            return false;
        }
        return switch (role) {
            case PROVINCIAL_ADMIN, NATIONAL_VIEWER -> true;
            case PROVINCIAL_SUPERVISOR -> hazard == incidentHazard;
            case WARD_RECORDER -> hazard == incidentHazard
                    && ward != null && incidentWard != null
                    && ward.trim().equalsIgnoreCase(incidentWard.trim());
        };
    }

    // ---------- getters / setters ----------

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Role getRole() { return role; }
    public HazardType getHazard() { return hazard; }
    public String getWard() { return ward; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isReceiveAlerts() { return receiveAlerts; }
    public void setReceiveAlerts(boolean receiveAlerts) { this.receiveAlerts = receiveAlerts; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
