package uz.bozor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter
@Entity @Table(name = "items")
public class Item {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private User seller;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String category;
    @Column(name = "condition_text", nullable = false, length = 1000) private String condition;
    @Column(nullable = false) private LocalDate productionDate;
    @Column(nullable = false) private Long price;
    @Column(nullable = false) private String imageUrl;
    @Column(length = 500) private String extraImages;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ItemStatus status = ItemStatus.ACTIVE;
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
