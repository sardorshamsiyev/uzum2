package uz.bozor.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uz.bozor.dto.Dto.*;
import uz.bozor.entity.PendingReg;
import uz.bozor.entity.User;
import uz.bozor.repo.PendingRegRepository;
import uz.bozor.repo.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final UserRepository users;
    private final PendingRegRepository pend;
    private final PasswordEncoder enc;
    private final String botToken;
    private final String botUsername;

    public MeController(UserRepository users, PendingRegRepository pend, PasswordEncoder enc,
                        @Value("${app.telegram.bot-token:}") String botToken,
                        @Value("${app.telegram.bot-username:}") String botUsername) {
        this.users = users;
        this.pend = pend;
        this.enc = enc;
        this.botToken = botToken;
        this.botUsername = botUsername.replace("@", "").trim();
    }

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

    @PatchMapping("/password")
    public void password(@AuthenticationPrincipal User me, @Valid @RequestBody PasswordReq r) {
        if (!enc.matches(r.oldPassword(), me.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hozirgi parol noto'g'ri");
        me.setPasswordHash(enc.encode(r.newPassword()));
        users.save(me);
    }

    @PatchMapping("/name")
    public UserRes name(@AuthenticationPrincipal User me, @Valid @RequestBody NameReq r) {
        me.setFullName(r.fullName().trim());
        return UserRes.of(users.save(me));
    }

    /** Raqamni o'zgartirish 1-qadam: yangi raqam Telegram orqali tasdiqlanadi. */
    @PostMapping("/phone/start")
    @Transactional
    public TgStartRes phoneStart(@AuthenticationPrincipal User me, @Valid @RequestBody PhoneReq r) {
        if (botToken.isBlank() || botUsername.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram bot hali sozlanmagan");
        String phone = AuthController.normalize(r.phone());
        if (phone.equals(me.getPhone()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu sizning hozirgi raqamingiz");
        if (users.existsByPhone(phone))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu raqam boshqa akkauntda ishlatilgan");
        pend.deleteByUserId(me.getId());
        pend.deleteByCreatedAtBefore(Instant.now().minus(Duration.ofMinutes(30)));
        pend.deleteByPhone(phone);
        PendingReg p = new PendingReg();
        p.setId(UUID.randomUUID().toString().replace("-", ""));
        p.setPhone(phone);
        p.setFullName(me.getFullName());
        p.setPasswordHash(me.getPasswordHash());
        p.setUserId(me.getId());
        p.setPurpose("PHONE");
        p.setCreatedAt(Instant.now());
        pend.save(p);
        return new TgStartRes(p.getId(), "https://t.me/" + botUsername + "?start=" + p.getId());
    }

    /** 2-qadam: Telegramdan kelgan kod kiritiladi, to'g'ri bo'lsa raqam almashadi. */
    @PostMapping("/phone/verify")
    public UserRes phoneVerify(@AuthenticationPrincipal User me, @Valid @RequestBody TgVerifyReq r) {
        PendingReg p = pend.findById(r.token()).orElse(null);
        if (p == null || p.getUserId() == null || !"PHONE".equals(p.getPurpose()) || !p.getUserId().equals(me.getId())
                || p.getCreatedAt() == null || p.getCreatedAt().isBefore(Instant.now().minus(Duration.ofMinutes(15))))
            throw new ResponseStatusException(HttpStatus.GONE, "Vaqt tugadi. Raqamni o'zgartirishni qaytadan boshlang");
        if (!p.isVerified() || p.getCode() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avval Telegram botda yangi raqamingizni tasdiqlang");
        if (!p.getCode().equals(r.code().trim())) {
            int a = (p.getAttempts() == null ? 0 : p.getAttempts()) + 1;
            if (a >= 5) {
                pend.delete(p);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Juda ko'p xato urinish. Qaytadan boshlang");
            }
            p.setAttempts(a);
            pend.save(p);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kod noto'g'ri");
        }
        if (users.existsByPhone(p.getPhone())) {
            pend.delete(p);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu raqam boshqa akkauntda ishlatilgan");
        }
        me.setPhone(p.getPhone());
        users.save(me);
        pend.delete(p);
        return UserRes.of(me);
    }
}
