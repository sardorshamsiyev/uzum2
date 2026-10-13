package uz.bozor.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uz.bozor.dto.Dto.*;
import uz.bozor.entity.VideoLesson;
import uz.bozor.repo.VideoLessonRepository;
import java.util.List;

@RestController
@RequestMapping("/api")
public class VideoController {
    private final VideoLessonRepository repo;

    public VideoController(VideoLessonRepository repo) { this.repo = repo; }

    private static VideoRes res(VideoLesson v) { return new VideoRes(v.getId(), v.getTitle(), v.getUrl()); }

    /** Hamma ko'ra oladi (kirmagan odam ham). */
    @GetMapping("/videos")
    public List<VideoRes> list() {
        return repo.findAllByOrderByCreatedAtAsc().stream().map(VideoController::res).toList();
    }

    @PostMapping("/admin/videos")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public VideoRes add(@Valid @RequestBody VideoReq r) {
        VideoLesson v = new VideoLesson();
        v.setTitle(r.title().trim());
        v.setUrl(r.url().trim());
        return res(repo.save(v));
    }

    @DeleteMapping("/admin/videos/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video topilmadi");
        repo.deleteById(id);
    }
}
