package uz.bozor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** Video darslik: faqat sarlavha va tashqi havola (YouTube, Telegram) saqlanadi. */
@Getter @Setter
@Entity @Table(name = "video_lessons")
public class VideoLesson {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 500) private String url;
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
