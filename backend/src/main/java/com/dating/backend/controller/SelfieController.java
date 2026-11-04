package com.dating.backend.controller;
import com.dating.backend.matching.SelfieReviewService;import com.dating.backend.admin.AdminService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;import org.springframework.http.*;import java.security.Principal;import java.util.*;import jakarta.servlet.http.HttpServletRequest;
@RestController @RequiredArgsConstructor public class SelfieController {
 private final SelfieReviewService selfies;private final AdminService admins;
 @GetMapping("/api/admin/selfies/pending") public SelfieReviewService.PendingQueue pending(@RequestParam(defaultValue="0") int page){return selfies.pending(page);}
 public record ReviewRequest(String reason){}
 @GetMapping("/api/selfie/request") public SelfieReviewService.RequestStatus requested(Principal p){return selfies.requestStatus(p.getName());}
 @GetMapping("/api/admin/selfies/user/{userId}/request") public SelfieReviewService.RequestStatus requestedForUser(@PathVariable Long userId){return selfies.requestStatusForUser(userId);}
 @PostMapping("/api/admin/selfies/user/{userId}/request") public void requestReview(@PathVariable Long userId,@RequestBody ReviewRequest r){selfies.requestReview(userId,r.reason());}
 @DeleteMapping("/api/admin/selfies/user/{userId}/request") public void cancelRequest(@PathVariable Long userId){selfies.cancelRequest(userId);}
 @GetMapping("/api/selfie") public SelfieReviewService.View status(Principal p){return selfies.status(p.getName());}
 @DeleteMapping("/api/selfie") public void withdraw(Principal p){selfies.withdrawConsent(p.getName());}
 @PostMapping("/api/selfie/start") public SelfieReviewService.View start(Principal p){return selfies.start(p.getName());}
 @PostMapping("/api/selfie/{id}") public SelfieReviewService.View submit(Principal p,@PathVariable Long id,@RequestParam boolean consent,@RequestParam MultipartFile file){return selfies.submit(p.getName(),id,consent,file);}
 @GetMapping("/api/admin/selfies/user/{userId}") public SelfieReviewService.View forUser(@PathVariable Long userId){return selfies.forUser(userId);}
 @GetMapping("/api/admin/selfies") public List<SelfieReviewService.View> list(@RequestParam(defaultValue="0") int page){return selfies.list(page);}
 @GetMapping("/api/admin/selfies/{id}/photo") public ResponseEntity<byte[]> photo(@PathVariable Long id,@RequestParam(defaultValue="false") boolean reference,HttpServletRequest request){var p=(AdminService.Principal)request.getAttribute("admin");var bytes=selfies.photo(id,reference);admins.audit(p.id(),p.username(),"SELFIE_PHOTO_VIEW "+id+(reference?" REFERENCE":" SELFIE"),200);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").contentType(MediaType.IMAGE_PNG).body(bytes);}
 public record Decision(boolean approve,String note){}
 @PutMapping("/api/admin/selfies/{id}") public SelfieReviewService.View decide(@PathVariable Long id,@RequestBody Decision d){return selfies.decide(id,d.approve(),d.note());}
}
