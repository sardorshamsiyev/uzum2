package uz.bozor.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uz.bozor.dto.Dto.*;
import uz.bozor.entity.Role;
import uz.bozor.entity.User;
import uz.bozor.repo.UserRepository;
import uz.bozor.security.JwtService;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder enc;
    private final JwtService jwt;
    private final String ownerPhone;

    public AuthController(UserRepository users, PasswordEncoder enc, JwtService jwt,
                          @Value("${app.owner-phone}") String ownerPhone) {
        this.users = users;
        this.enc = enc;
        this.jwt = jwt;
        this.ownerPhone = ownerPhone.replaceAll("\\s", "");
    }

    /** "90 123 45 67" yoki "+998901234567" -> "+998901234567" */
    static String normalize(String raw) {
        String d = raw.replaceAll("\\D", "");
        if (d.length() < 9) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefon: +998 dan keyin 9 ta raqam kiriting");
        return "+998" + d.substring(d.length() - 9);
    }

    @PostMapping("/register")
    public AuthRes register(@Valid @RequestBody RegisterReq r) {
        String phone = normalize(r.phone());
        if (users.existsByPhone(phone))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu raqam allaqachon ro'yxatdan o'tgan");
        User u = new User();
        u.setFullName(r.fullName().trim());
        u.setPhone(phone);
        u.setPasswordHash(enc.encode(r.password()));
        u.setNickname(freeNick(r.fullName()));
        u.setRole(phone.equals(ownerPhone) ? Role.OWNER : Role.USER);
        users.save(u);
        return new AuthRes(jwt.create(u), UserRes.of(u));
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
