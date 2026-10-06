package uz.bozor.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.bozor.entity.Role;
import uz.bozor.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsByNickname(String nickname);
    Optional<User> findFirstByRole(Role role);
}
