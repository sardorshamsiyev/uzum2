package uz.bozor.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uz.bozor.dto.Dto.*;
import uz.bozor.entity.*;
import uz.bozor.repo.ItemRepository;
import uz.bozor.security.Perm;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemRepository items;
    private final Perm perm;

    public ItemController(ItemRepository items, Perm perm) {
        this.items = items;
        this.perm = perm;
    }

    private static List<ItemRes> map(List<Item> l) { return l.stream().map(ItemRes::of).toList(); }

    private Item find(Long id) {
        return items.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "E'lon topilmadi"));
    }

    @PostMapping
    public ItemRes create(@AuthenticationPrincipal User me, @Valid @RequestBody ItemReq r) {
        Item i = new Item();
        i.setSeller(me);
        i.setName(r.name().trim());
        i.setCategory(r.category());
        i.setCondition(r.condition().trim());
        i.setProductionDate(r.productionDate());
        i.setPrice(r.price());
        i.setImageUrl(r.imageUrl());
        if (r.extraImages() != null)
            i.setExtraImages(String.join(",", r.extraImages().stream().filter(x -> x != null && !x.isBlank()).limit(2).toList()));
        i.setStatus(me.getRole() == Role.USER ? ItemStatus.PENDING : ItemStatus.ACTIVE);
        return ItemRes.of(items.save(i));
    }

    @GetMapping
    public List<ItemRes> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return map(items.feed(ItemStatus.ACTIVE, Role.OWNER, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))));
    }

    @GetMapping("/random")
    public List<ItemRes> random(@RequestParam(defaultValue = "20") int size) {
        return map(items.random(Math.min(Math.max(size, 1), 50)));
    }

    @GetMapping("/search")
    public List<ItemRes> search(@RequestParam String q, @RequestParam(defaultValue = "item") String by) {
        String s = q.trim().replace("@", "");
        if (s.isEmpty()) return List.of();
        return map("user".equals(by) ? items.searchBySeller(ItemStatus.ACTIVE, s) : items.searchByItem(ItemStatus.ACTIVE, s));
    }

    @GetMapping("/my")
    public List<ItemRes> my(@AuthenticationPrincipal User me) {
        return map(items.findBySellerAndStatusNotOrderByCreatedAtDesc(me, ItemStatus.REMOVED));
    }

    @GetMapping("/{id}")
    public ItemRes one(@PathVariable Long id) { return ItemRes.of(find(id)); }

    @PatchMapping("/{id}/sold")
    public ItemRes sold(@AuthenticationPrincipal User me, @PathVariable Long id) {
        Item i = find(id);
        if (!i.getSeller().getId().equals(me.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Faqat e'lon egasi belgilay oladi");
        if (i.getStatus() != ItemStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E'lon sotuvda emas");
        i.setStatus(ItemStatus.SOLD);
        return ItemRes.of(items.save(i));
    }

    @DeleteMapping("/{id}")
    public ItemRes remove(@AuthenticationPrincipal User me, @PathVariable Long id) {
        Item i = find(id);
        boolean own = i.getSeller().getId().equals(me.getId());
        if (!own && !perm.canAct(me, i.getSeller()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu e'lonni o'chirishga ruxsat yo'q");
        i.setStatus(ItemStatus.REMOVED); // bazadan o'chirilmaydi: statistika uchun
        return ItemRes.of(items.save(i));
    }
}
