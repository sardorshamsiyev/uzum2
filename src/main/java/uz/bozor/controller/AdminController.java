package uz.bozor.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uz.bozor.dto.Dto.*;
import uz.bozor.entity.*;
import uz.bozor.repo.*;
import uz.bozor.security.Perm;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminController {
    private final UserRepository users;
    private final ItemRepository items;
    private final FeedbackRepository feedback;
    private final Perm perm;

    public AdminController(UserRepository users, ItemRepository items, FeedbackRepository feedback, Perm perm) {
        this.users = users;
        this.items = items;
        this.feedback = feedback;
        this.perm = perm;
    }

    private User target(User me, Long id) {
        User t = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foydalanuvchi topilmadi"));
        if (!perm.canAct(me, t)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu foydalanuvchi ustida amal qilishga ruxsat yo'q");
        return t;
    }

    @GetMapping("/users")
    public List<UserRes> allUsers() { return users.findAll().stream().map(UserRes::of).toList(); }

    @GetMapping("/stats")
    public StatsRes stats() {
        return new StatsRes(users.count(), items.count(), items.countByStatus(ItemStatus.REMOVED), items.countByStatus(ItemStatus.SOLD));
    }

    @PatchMapping("/users/{id}/block")
    public UserRes block(@AuthenticationPrincipal User me, @PathVariable Long id) {
        User t = target(me, id);
        t.setBlocked(!t.isBlocked());
        return UserRes.of(users.save(t));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public void delete(@AuthenticationPrincipal User me, @PathVariable Long id) {
        User t = target(me, id);
        items.deleteBySeller(t);
        feedback.deleteByAuthor(t);
        users.delete(t);
    }

    @PatchMapping("/users/{id}/make-admin")
    public UserRes makeAdmin(@AuthenticationPrincipal User me, @PathVariable Long id) {
        User t = target(me, id);
        if (t.getRole() != Role.USER) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu foydalanuvchi allaqachon admin");
        t.setRole(Role.ADMIN);
        return UserRes.of(users.save(t));
    }

    /** Adminlikdan faqat EGA oladi. */
    @PatchMapping("/users/{id}/remove-admin")
    @PreAuthorize("hasRole('OWNER')")
    public UserRes removeAdmin(@AuthenticationPrincipal User me, @PathVariable Long id) {
        User t = target(me, id);
        if (t.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu foydalanuvchi admin emas");
        t.setRole(Role.USER);
        return UserRes.of(users.save(t));
    }

    /** Fikrlarni faqat EGA ko'radi. */
    @GetMapping("/feedback")
    @PreAuthorize("hasRole('OWNER')")
    public List<FeedbackRes> feedbackList() {
        return feedback.findAllByOrderByCreatedAtDesc().stream().map(FeedbackRes::of).toList();
    }

    @PatchMapping("/feedback/read")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public void markRead() { feedback.markAllSeen(); }
}
