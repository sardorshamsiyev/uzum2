package uz.bozor.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uz.bozor.dto.Dto.*;
import uz.bozor.entity.User;
import uz.bozor.repo.UserRepository;
import java.time.Instant;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final UserRepository users;

    public MeController(UserRepository users) { this.users = users; }

    @GetMapping
    public UserRes me(@AuthenticationPrincipal User me) { return UserRes.of(me); }

    /** Ilova o'rnatilgani (yoki ilova sifatida ochilgani) haqida belgi. */
    @PostMapping("/installed")
    public void installed(@AuthenticationPrincipal User me) {
        if (me.getInstalledAt() == null) {
            me.setInstalledAt(Instant.now());
            users.save(me);
        }
    }

    @PatchMapping("/avatar")
    public UserRes avatar(@AuthenticationPrincipal User me, @Valid @RequestBody AvatarReq r) {
        me.setAvatarUrl(r.avatarUrl());
        return UserRes.of(users.save(me));
    }

    @PatchMapping("/nickname")
    public UserRes nickname(@AuthenticationPrincipal User me, @Valid @RequestBody NickReq r) {
        if (!r.nickname().equals(me.getNickname()) && users.existsByNickname(r.nickname()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu nickname band");
        me.setNickname(r.nickname());
        return UserRes.of(users.save(me));
    }
}
