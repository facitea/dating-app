package com.dating.backend.controller;
import com.dating.backend.matching.ProfilePhotoHistoryService;import com.dating.backend.admin.AdminService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import jakarta.servlet.http.HttpServletRequest;import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/api/admin/photo-history") public class ProfilePhotoHistoryController {
 private final ProfilePhotoHistoryService history;private final AdminService admins;
 @GetMapping("/user/{userId}") public List<ProfilePhotoHistoryService.View> list(@PathVariable Long userId,@RequestParam(defaultValue="0") int page){return history.list(userId,page);}
 @GetMapping("/{id}/photo") public ResponseEntity<byte[]> photo(@PathVariable Long id,HttpServletRequest request){var p=(AdminService.Principal)request.getAttribute("admin");var bytes=history.photo(id);admins.audit(p.id(),p.username(),"PROFILE_PHOTO_HISTORY_VIEW "+id,200);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").contentType(MediaType.IMAGE_JPEG).body(bytes);}
}
