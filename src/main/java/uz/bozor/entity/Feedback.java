package uz.bozor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter
@Entity @Table(name = "feedback")
public class Feedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private User author;
    @Column(nullable = false, length = 1000) private String text;
    private boolean seen;
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
