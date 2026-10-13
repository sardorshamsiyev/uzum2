package uz.bozor.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.bozor.entity.VideoLesson;
import java.util.List;

public interface VideoLessonRepository extends JpaRepository<VideoLesson, Long> {
    List<VideoLesson> findAllByOrderByCreatedAtAsc();
}
