package uz.bozor.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.bozor.dto.Dto.*;
import uz.bozor.entity.Feedback;
import uz.bozor.entity.Role;
import uz.bozor.entity.User;
import uz.bozor.repo.FeedbackRepository;
import uz.bozor.repo.UserRepository;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FeedbackController {
    private final FeedbackRepository feedback;
    private final UserRepository users;
    private final String ownerPhone;

    public FeedbackController(FeedbackRepository feedback, UserRepository users, @Value("${app.owner-phone}") String ownerPhone) {
        this.feedback = feedback;
        this.users = users;
        this.ownerPhone = ownerPhone;
    }

    @PostMapping("/feedback")
    public FeedbackRes send(@AuthenticationPrincipal User me, @Valid @RequestBody FeedbackReq r) {
        Feedback f = new Feedback();
        f.setAuthor(me);
        f.setText(r.text().trim());
        return FeedbackRes.of(feedback.save(f));
    }

    /** "Biz bilan bog'lanish": ochiq, eganing raqamini qaytaradi. */
    @GetMapping("/contact")
    public Map<String, String> contact() {
        String phone = users.findFirstByRole(Role.OWNER).map(User::getPhone).orElse(ownerPhone);
        return Map.of("phone", phone);
    }
}
