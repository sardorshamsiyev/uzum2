package uz.bozor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** Telegram orqali tasdiqlanishini kutayotgan ro'yxatdan o'tish so'rovi. */
@Getter @Setter
@Entity @Table(name = "pending_reg")
public class PendingReg {
    @Id
    private String id;
    @Column(nullable = false) private String phone;
    @Column(nullable = false) private String fullName;
    @Column(nullable = false) private String passwordHash;
    private Instant createdAt;
    private boolean verified;
    private Long chatId;
}
