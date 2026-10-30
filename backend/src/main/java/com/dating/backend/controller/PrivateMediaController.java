package com.dating.backend.controller;
import com.dating.backend.matching.PrivateMediaService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import org.springframework.http.*;import org.springframework.core.io.*;import jakarta.servlet.http.HttpServletRequest;import java.nio.file.Path;
@RestController @RequiredArgsConstructor public class PrivateMediaController {
 private final PrivateMediaService media;
 @GetMapping({"/api/media/{folder}/{file}","/uploads/{folder}/{file}"}) public ResponseEntity<Resource> member(@PathVariable String folder,@PathVariable String file,Authentication a){return response(media.read(a.getName(),folder+"/"+file));}
 @GetMapping("/api/admin/media/{folder}/{file}") public ResponseEntity<Resource> admin(@PathVariable String folder,@PathVariable String file){com.dating.backend.admin.AdminService.requireSession();return response(media.path(folder+"/"+file));}
 private ResponseEntity<Resource> response(Path path){String name=path.toString();MediaType type=name.endsWith(".png")?MediaType.IMAGE_PNG:name.endsWith(".webp")?MediaType.parseMediaType("image/webp"):MediaType.IMAGE_JPEG;return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header("Cross-Origin-Resource-Policy","same-origin").contentType(type).body(new FileSystemResource(path));}
}
