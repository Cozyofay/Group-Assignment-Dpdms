package zw.ac.uz.dpdms.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<UserAccount> findAllByOrderByRoleAscUsernameAsc();

    List<UserAccount> findByEnabledTrueAndReceiveAlertsTrue();
}
