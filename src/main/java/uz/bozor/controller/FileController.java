package uz.bozor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private static final Map<String, String> EXT = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    @Value("${app.upload-dir}") private String uploadDir;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile f) throws IOException {
        String ext = EXT.get(f.getContentType());
        if (ext == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faqat jpg, png yoki webp rasm yuklang");
        if (f.isEmpty() || f.getSize() > 5 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rasm 5 MB dan oshmasligi kerak");
        Path dir = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(dir);
        String name = UUID.randomUUID() + "." + ext;
        f.transferTo(dir.resolve(name));
        return Map.of("url", "/uploads/" + name);
    }
}
