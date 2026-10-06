package uz.bozor.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import uz.bozor.entity.Feedback;
import uz.bozor.entity.User;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderByCreatedAtDesc();
    void deleteByAuthor(User author);

    @Modifying
    @Query("update Feedback f set f.seen = true")
    void markAllSeen();
}
