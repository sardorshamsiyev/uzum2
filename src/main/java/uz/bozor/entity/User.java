package uz.bozor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String fullName;
    @Column(nullable = false, unique = true) private String phone;
    @Column(nullable = false) private String passwordHash;
    @Column(nullable = false, unique = true) private String nickname;
    private String avatarUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role = Role.USER;
    private boolean blocked;
    private Instant createdAt;
    private Instant installedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
