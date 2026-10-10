package uz.bozor.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uz.bozor.dto.Dto.*;
import uz.bozor.entity.PendingReg;
import uz.bozor.entity.Role;
import uz.bozor.entity.User;
import uz.bozor.repo.PendingRegRepository;
import uz.bozor.repo.UserRepository;
import uz.bozor.security.JwtService;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PendingRegRepository pend;
    private final PasswordEncoder enc;
    private final JwtService jwt;
    private final String ownerPhone;
    private final String botToken;
    private final String botUsername;

    public AuthController(UserRepository users, PendingRegRepository pend, PasswordEncoder enc, JwtService jwt,
                          @Value("${app.owner-phone}") String ownerPhone,
                          @Value("${app.telegram.bot-token:}") String botToken,
                          @Value("${app.telegram.bot-username:}") String botUsername) {
        this.users = users;
        this.pend = pend;
        this.enc = enc;
        this.jwt = jwt;
        this.ownerPhone = ownerPhone.replaceAll("\\s", "");
        this.botToken = botToken;
        this.botUsername = botUsername.replace("@", "").trim();
    }

    /** "90 123 45 67" yoki "+998901234567" -> "+998901234567" */
    static String normalize(String raw) {
        String d = raw.replaceAll("\\D", "");
        if (d.length() < 9) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefon: +998 dan keyin 9 ta raqam kiriting");
        return "+998" + d.substring(d.length() - 9);
    }

    /** 1-qadam: ma'lumotlar saqlanadi, Telegram havolasi qaytariladi. */
    @PostMapping("/tg/start")
    @Transactional
    public TgStartRes tgStart(@Valid @RequestBody RegisterReq r) {
        if (botToken.isBlank() || botUsername.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram bot hali sozlanmagan");
        String phone = normalize(r.phone());
        if (users.existsByPhone(phone))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu raqam allaqachon ro'yxatdan o'tgan");
        pend.deleteByCreatedAtBefore(Instant.now().minus(Duration.ofMinutes(30)));
        pend.deleteByPhone(phone);
        PendingReg p = new PendingReg();
        p.setId(UUID.randomUUID().toString().replace("-", ""));
        p.setPhone(phone);
        p.setFullName(r.fullName().trim());
        p.setPasswordHash(enc.encode(r.password()));
        p.setCreatedAt(Instant.now());
        pend.save(p);
        return new TgStartRes(p.getId(), "https://t.me/" + botUsername + "?start=" + p.getId());
    }

    /** 2-qadam: sayt shuni har 2 soniyada so'raydi. Tasdiqlansa, akkaunt ochiladi. */
    @GetMapping("/tg/status/{token}")
    public TgStatusRes tgStatus(@PathVariable String token) {
        PendingReg p = pend.findById(token).orElse(null);
        if (p == null || p.getCreatedAt() == null || p.getCreatedAt().isBefore(Instant.now().minus(Duration.ofMinutes(15))))
            return new TgStatusRes("EXPIRED", null);
        if (!p.isVerified()) return new TgStatusRes("PENDING", null);
        if (users.existsByPhone(p.getPhone())) {
            pend.delete(p);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu raqam allaqachon ro'yxatdan o'tgan");
        }
        User u = new User();
        u.setFullName(p.getFullName());
        u.setPhone(p.getPhone());
        u.setPasswordHash(p.getPasswordHash());
        u.setNickname(freeNick(p.getFullName()));
        u.setRole(p.getPhone().equals(ownerPhone) ? Role.OWNER : Role.USER);
        users.save(u);
        pend.delete(p);
        return new TgStatusRes("VERIFIED", new AuthRes(jwt.create(u), UserRes.of(u)));
    }

    @PostMapping("/login")
    public AuthRes login(@Valid @RequestBody LoginReq r) {
        User u = users.findByPhone(normalize(r.phone())).orElse(null);
        if (u == null || !enc.matches(r.password(), u.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Telefon yoki parol xato");
        if (u.isBlocked())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Siz bloklangansiz. Ega bilan bog'laning.");
        return new AuthRes(jwt.create(u), UserRes.of(u));
    }

    private String freeNick(String fullName) {
        String base = fullName.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.length() < 3) base = "user";
        if (base.length() > 20) base = base.substring(0, 20);
        String nick = base;
        while (users.existsByNickname(nick)) nick = base + "_" + ThreadLocalRandom.current().nextInt(1000, 10000);
        return nick;
    }
}
