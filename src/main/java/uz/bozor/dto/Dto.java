package uz.bozor.dto;

import jakarta.validation.constraints.*;
import uz.bozor.entity.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public final class Dto {
    private Dto() {}

    public record RegisterReq(
            @NotBlank @Size(min = 2, max = 80) String fullName,
            @NotBlank String phone,
            @NotBlank @Size(min = 6, max = 72, message = "Parol kamida 6 ta belgi bo'lsin") String password) {}

    public record LoginReq(@NotBlank String phone, @NotBlank String password) {}

    public record UserRes(Long id, String fullName, String phone, String nickname, String avatarUrl, Role role, boolean blocked) {
        public static UserRes of(User u) {
            return new UserRes(u.getId(), u.getFullName(), u.getPhone(), u.getNickname(), u.getAvatarUrl(), u.getRole(), u.isBlocked());
        }
    }

    public record AuthRes(String token, UserRes user) {}

    public record SellerRes(Long id, String nickname, String fullName, String phone, String avatarUrl) {}

    public record ItemReq(
            @NotBlank @Size(max = 80) String name,
            @NotBlank String category,
            @NotBlank @Size(max = 1000, message = "Holati juda uzun") String condition,
            @NotNull(message = "Sanani kiriting") @PastOrPresent(message = "Kelajak sanasini kiritib bo'lmaydi") LocalDate productionDate,
            @NotNull @Min(value = 10000, message = "Narx kamida 10 000 so'm bo'lishi kerak") Long price,
            @NotBlank(message = "Rasm joylash majburiy") String imageUrl,
            @Size(max = 2, message = "Ko'pi bilan 3 ta rasm joylash mumkin") List<String> extraImages) {}

    public record ItemRes(Long id, String name, String category, String condition, LocalDate productionDate,
                          Long price, String imageUrl, ItemStatus status, Instant createdAt, SellerRes seller, List<String> images) {
        public static ItemRes of(Item i) {
            User s = i.getSeller();
            List<String> imgs = new ArrayList<>();
            imgs.add(i.getImageUrl());
            if (i.getExtraImages() != null)
                for (String x : i.getExtraImages().split(",")) if (!x.isBlank()) imgs.add(x);
            return new ItemRes(i.getId(), i.getName(), i.getCategory(), i.getCondition(), i.getProductionDate(),
                    i.getPrice(), i.getImageUrl(), i.getStatus(), i.getCreatedAt(),
                    new SellerRes(s.getId(), s.getNickname(), s.getFullName(), s.getPhone(), s.getAvatarUrl()), imgs);
        }
    }

    public record AvatarReq(@NotBlank String avatarUrl) {}

    public record NickReq(@NotBlank @Pattern(regexp = "^[a-z0-9_.]{3,30}$",
            message = "Nickname: 3-30 ta kichik lotin harf, raqam, _ yoki .") String nickname) {}

    public record FeedbackReq(@NotBlank @Size(max = 1000) String text) {}

    public record FeedbackRes(Long id, String authorNickname, String text, boolean seen, Instant createdAt) {
        public static FeedbackRes of(Feedback f) {
            return new FeedbackRes(f.getId(), f.getAuthor().getNickname(), f.getText(), f.isSeen(), f.getCreatedAt());
        }
    }

    public record StatsRes(long users, long posted, long removed, long sold) {}
}
