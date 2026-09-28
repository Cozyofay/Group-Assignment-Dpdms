package zw.ac.uz.dpdms.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import zw.ac.uz.dpdms.auth.user.UserAccount;
import zw.ac.uz.dpdms.auth.user.UserAccountRepository;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;

import java.util.List;

/**
 * Seed data: on first start (empty users table) creates one user for every role/hazard combination
 * so the whole system can be demonstrated. All share SEED_DEFAULT_PASSWORD from .env, so no password
 * is hard-coded in the source.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private record SeedUser(String username, String fullName, Role role, HazardType hazard, String ward) {
    }

    private static final List<SeedUser> SEED_USERS = List.of(
            new SeedUser("admin", "Provincial Administrator", Role.PROVINCIAL_ADMIN, null, null),
            new SeedUser("national", "National Disaster Officer", Role.NATIONAL_VIEWER, null, null),

            new SeedUser("supervisor.flood", "Flood Supervisor", Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null),
            new SeedUser("supervisor.drought", "Drought Supervisor", Role.PROVINCIAL_SUPERVISOR, HazardType.DROUGHT, null),
            new SeedUser("supervisor.fire", "Fire Supervisor", Role.PROVINCIAL_SUPERVISOR, HazardType.FIRE, null),
            new SeedUser("supervisor.zoonotic", "Zoonotic Disease Supervisor", Role.PROVINCIAL_SUPERVISOR, HazardType.ZOONOTIC_DISEASE, null),
            new SeedUser("supervisor.mining", "Mining Accident Supervisor", Role.PROVINCIAL_SUPERVISOR, HazardType.MINING_ACCIDENT, null),

            new SeedUser("recorder.flood.ward1", "Flood Recorder Ward 1", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1"),
            new SeedUser("recorder.flood.ward2", "Flood Recorder Ward 2", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 2"),
            new SeedUser("recorder.drought.ward1", "Drought Recorder Ward 1", Role.WARD_RECORDER, HazardType.DROUGHT, "Ward 1"),
            new SeedUser("recorder.fire.ward1", "Fire Recorder Ward 1", Role.WARD_RECORDER, HazardType.FIRE, "Ward 1"),
            new SeedUser("recorder.zoonotic.ward1", "Zoonotic Recorder Ward 1", Role.WARD_RECORDER, HazardType.ZOONOTIC_DISEASE, "Ward 1"),
            new SeedUser("recorder.mining.ward1", "Mining Recorder Ward 1", Role.WARD_RECORDER, HazardType.MINING_ACCIDENT, "Ward 1"));

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String defaultPassword;

    public DataSeeder(UserAccountRepository repository, PasswordEncoder passwordEncoder,
                      @Value("${dpdms.seed.enabled:true}") boolean enabled,
                      @Value("${dpdms.seed.default-password:}") String defaultPassword) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.defaultPassword = defaultPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || repository.count() > 0) {
            return;
        }
        if (!StringUtils.hasText(defaultPassword)) {
            log.warn("SEED_DEFAULT_PASSWORD is not set in .env - no demo users were created.");
            return;
        }
        String hash = passwordEncoder.encode(defaultPassword);
        for (SeedUser seed : SEED_USERS) {
            UserAccount user = new UserAccount();
            user.setUsername(seed.username());
            user.setPasswordHash(hash);
            user.setFullName(seed.fullName());
            user.setEmail(seed.username() + "@dpdms.local");
            user.applyScope(seed.role(), seed.hazard(), seed.ward());
            repository.save(user);
        }
        log.info("Created {} demo users (password = SEED_DEFAULT_PASSWORD from .env)", SEED_USERS.size());
    }
}
