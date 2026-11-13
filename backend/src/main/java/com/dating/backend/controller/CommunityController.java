package com.dating.backend.controller;
import com.dating.backend.matching.CommunityService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import java.util.*;
@RestController @RequiredArgsConstructor public class CommunityController {
 private final CommunityService service;
 @GetMapping("/api/community") public List<CommunityService.View> list(Authentication a,@RequestParam(defaultValue="ALL")String category,@RequestParam(defaultValue="0")long before,@RequestParam(defaultValue="false")boolean mine){return service.list(a.getName(),category,before,mine);}
 @GetMapping("/api/community/{id}") public CommunityService.Thread thread(Authentication a,@PathVariable Long id){return service.thread(a.getName(),id);}
 @PostMapping(value="/api/community",consumes="application/json") public CommunityService.View create(Authentication a,@RequestBody CommunityService.Write w){return service.create(a.getName(),w);}
 @DeleteMapping("/api/community/{id}") public void delete(Authentication a,@PathVariable Long id){service.delete(a.getName(),id);}
 @PostMapping(value="/api/community",consumes="multipart/form-data") public CommunityService.View createPhoto(Authentication a,@RequestParam String category,@RequestParam String title,@RequestParam String body,@RequestPart(required=false) org.springframework.web.multipart.MultipartFile photo){return service.createWithPhoto(a.getName(),new CommunityService.Write(category,title,body,null),photo);}
 private org.springframework.http.ResponseEntity<byte[]> image(byte[] bytes){return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.IMAGE_JPEG).header("Cache-Control","private, no-store").header("X-Content-Type-Options","nosniff").body(bytes);}
 @GetMapping("/api/community/{id}/photo") public org.springframework.http.ResponseEntity<byte[]> photo(Authentication a,@PathVariable Long id){return image(service.photo(a.getName(),id));}
 @GetMapping("/api/admin/community/{id}/photo") public org.springframework.http.ResponseEntity<byte[]> adminPhoto(@PathVariable Long id){return image(service.adminPhoto(id));}
 public record Report(String reason){} public record Moderate(boolean hidden){}
 @PostMapping("/api/community/{id}/report") public void report(Authentication a,@PathVariable Long id,@RequestBody Report r){service.report(a.getName(),id,r.reason());}
 @GetMapping("/api/admin/community/reports") public List<CommunityService.Flag> reports(@RequestParam(defaultValue="0")long before){return service.reports(before);}
 @PutMapping("/api/admin/community/{id}") public void moderate(@PathVariable Long id,@RequestBody Moderate r){service.moderate(id,r.hidden());}
}
