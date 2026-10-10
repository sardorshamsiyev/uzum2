package uz.bozor.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import uz.bozor.entity.PendingReg;
import java.time.Instant;
import java.util.Optional;

public interface PendingRegRepository extends JpaRepository<PendingReg, String> {
    Optional<PendingReg> findFirstByChatIdAndVerifiedFalseOrderByCreatedAtDesc(Long chatId);

    @Transactional
    void deleteByPhone(String phone);

    @Transactional
    void deleteByCreatedAtBefore(Instant t);
}
