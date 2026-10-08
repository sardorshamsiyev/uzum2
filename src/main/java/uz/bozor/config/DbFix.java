package uz.bozor.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Yangi status qiymatlari (PENDING, REJECTED) uchun eski CHECK cheklovini olib tashlaydi. */
@Component
public class DbFix implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public DbFix(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE items DROP CONSTRAINT IF EXISTS items_status_check");
        } catch (Exception ignored) { }
    }
}
