package com.dating.backend.controller;
import com.dating.backend.matching.PhotoDuplicateService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/api/admin/photo-duplicates") public class PhotoDuplicateController {
 private final PhotoDuplicateService service;
 @GetMapping public List<PhotoDuplicateService.View> list(@RequestParam(defaultValue="false") boolean reviewed,@RequestParam(defaultValue="0") int page){return service.list(reviewed,page);}
 public record Decision(boolean reviewed){}
 @PutMapping("/{id}") public void review(@PathVariable Long id,@RequestBody Decision body){service.review(id,body.reviewed());}
}
