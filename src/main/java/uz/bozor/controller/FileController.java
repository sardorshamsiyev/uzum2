package uz.bozor.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uz.bozor.entity.Image;
import uz.bozor.repo.ImageRepository;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class FileController {
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final ImageRepository images;

    public FileController(ImageRepository images) { this.images = images; }

    @PostMapping(value = "/api/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile f) throws IOException {
        String ct = f.getContentType();
        if (ct == null || !TYPES.contains(ct))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faqat jpg, png yoki webp rasm yuklang");
        if (f.isEmpty() || f.getSize() > 5 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rasm 5 MB dan oshmasligi kerak");
        Image img = new Image();
        img.setId(UUID.randomUUID().toString());
        img.setContentType(ct);
        img.setData(f.getBytes());
        images.save(img);
        return Map.of("url", "/uploads/" + img.getId());
    }

    @GetMapping("/uploads/{id}")
    public ResponseEntity<byte[]> get(@PathVariable String id) {
        return images.findById(id)
                .map(i -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(i.getContentType()))
                        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                        .body(i.getData()))
                .orElse(ResponseEntity.notFound().build());
    }
}
