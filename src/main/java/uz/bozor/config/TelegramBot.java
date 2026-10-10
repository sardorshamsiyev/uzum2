package uz.bozor.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uz.bozor.entity.PendingReg;
import uz.bozor.repo.PendingRegRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Telegram bot: ro'yxatdan o'tishda telefon raqamni tasdiqlaydi (long polling). */
@Component
public class TelegramBot {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private final String token;
    private final PendingRegRepository repo;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    public TelegramBot(@Value("${app.telegram.bot-token:}") String token, PendingRegRepository repo) {
        this.token = token;
        this.repo = repo;
    }

    @PostConstruct
    void start() {
        if (token.isBlank()) { log.info("Telegram bot o'chiq (TELEGRAM_BOT_TOKEN berilmagan)"); return; }
        Thread t = new Thread(this::loop, "telegram-poll");
        t.setDaemon(true);
        t.start();
        log.info("Telegram bot ishga tushdi");
    }

    private void loop() {
        try { call("deleteWebhook", Map.of()); } catch (Exception e) { log.warn("deleteWebhook: {}", e.getClass().getSimpleName()); }
        long offset = 0;
        while (true) {
            try {
                JsonNode res = call("getUpdates", Map.of("timeout", 25, "offset", offset));
                if (!res.path("ok").asBoolean()) { sleep(5000); continue; }
                for (JsonNode u : res.path("result")) {
                    offset = u.path("update_id").asLong() + 1;
                    try { handle(u.path("message")); }
                    catch (Exception e) { log.warn("Xabarni qayta ishlashda xato: {}", e.getClass().getSimpleName()); }
                }
            } catch (Exception e) {
                log.warn("Telegram so'rovi xatosi: {}", e.getClass().getSimpleName());
                sleep(5000);
            }
        }
    }

    private void handle(JsonNode m) throws Exception {
        if (m.isMissingNode() || m.isNull()) return;
        long chatId = m.path("chat").path("id").asLong();
        long fromId = m.path("from").path("id").asLong();
        JsonNode c = m.path("contact");
        String text = m.path("text").asText("");

        if (!c.isMissingNode()) {                       // raqam ulashildi
            if (c.path("user_id").asLong() != fromId) {
                send(chatId, "Faqat o'zingizning raqamingizni yuboring: pastdagi tugmani bosing.", null);
                return;
            }
            PendingReg p = repo.findFirstByChatIdAndVerifiedFalseOrderByCreatedAtDesc(chatId).orElse(null);
            if (p == null || expired(p)) {
                send(chatId, "So'rov topilmadi yoki eskirgan. Saytga qaytib, ro'yxatdan o'tishni qaytadan boshlang.", Map.of("remove_keyboard", true));
                return;
            }
            String d = c.path("phone_number").asText("").replaceAll("\\D", "");
            String phone = d.length() >= 9 ? "+998" + d.substring(d.length() - 9) : "";
            if (phone.equals(p.getPhone())) {
                p.setVerified(true);
                repo.save(p);
                send(chatId, "✅ Raqamingiz tasdiqlandi! Saytga qaytishingiz mumkin.", Map.of("remove_keyboard", true));
            } else {
                send(chatId, "❌ Telegramdagi raqamingiz saytda kiritgan raqam bilan mos kelmadi. Saytga qaytib, to'g'ri raqamni kiriting.", Map.of("remove_keyboard", true));
            }
        } else if (text.startsWith("/start")) {
            String[] parts = text.split(" ", 2);
            String tk = parts.length > 1 ? parts[1].trim() : "";
            PendingReg p = tk.isEmpty() ? null : repo.findById(tk).orElse(null);
            if (p == null || expired(p)) {
                send(chatId, "Salom! Ro'yxatdan o'tish uchun avval saytda ism, raqam va parolni kiritib, \"Davom etish\" tugmasini bosing.", null);
                return;
            }
            p.setChatId(chatId);
            repo.save(p);
            Map<String, Object> kb = Map.of(
                    "keyboard", List.of(List.of(Map.of("text", "📱 Raqamni tasdiqlash", "request_contact", true))),
                    "resize_keyboard", true, "one_time_keyboard", true);
            send(chatId, "Raqamingizni tasdiqlash uchun pastdagi tugmani bosing.", kb);
        } else {
            send(chatId, "Ro'yxatdan o'tish uchun saytdagi \"Davom etish\" tugmasidan keyin chiqqan havola orqali kiring.", null);
        }
    }

    private static boolean expired(PendingReg p) {
        return p.getCreatedAt() == null || p.getCreatedAt().isBefore(Instant.now().minus(Duration.ofMinutes(15)));
    }

    private void send(long chatId, String text, Map<String, Object> markup) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        if (markup != null) body.put("reply_markup", markup);
        call("sendMessage", body);
    }

    private JsonNode call(String method, Map<String, Object> body) throws Exception {
        HttpRequest rq = HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot" + token + "/" + method))
                .timeout(Duration.ofSeconds(40))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)))
                .build();
        return om.readTree(http.send(rq, HttpResponse.BodyHandlers.ofString()).body());
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
