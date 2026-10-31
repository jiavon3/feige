package feige.repo;

import feige.domain.Role;
import feige.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsernameKey(String usernameKey);

    boolean existsByUsernameKey(String usernameKey);

    List<UserAccount> findByRole(Role role);

    List<UserAccount> findByIdNot(Long id);
}
