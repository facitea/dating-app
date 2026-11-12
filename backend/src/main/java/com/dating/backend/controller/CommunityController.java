package com.dating.backend.controller;
import com.dating.backend.matching.CommunityService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import java.util.*;
@RestController @RequiredArgsConstructor public class CommunityController {
 private final CommunityService service;
 @GetMapping("/api/community") public List<CommunityService.View> list(Authentication a,@RequestParam(defaultValue="ALL")String category,@RequestParam(defaultValue="0")long before){return service.list(a.getName(),category,before);}
 @GetMapping("/api/community/{id}") public CommunityService.Thread thread(Authentication a,@PathVariable Long id){return service.thread(a.getName(),id);}
 @PostMapping("/api/community") public CommunityService.View create(Authentication a,@RequestBody CommunityService.Write w){return service.create(a.getName(),w);}
 @DeleteMapping("/api/community/{id}") public void delete(Authentication a,@PathVariable Long id){service.delete(a.getName(),id);}
 public record Report(String reason){} public record Moderate(boolean hidden){}
 @PostMapping("/api/community/{id}/report") public void report(Authentication a,@PathVariable Long id,@RequestBody Report r){service.report(a.getName(),id,r.reason());}
 @GetMapping("/api/admin/community/reports") public List<CommunityService.Flag> reports(@RequestParam(defaultValue="0")long before){return service.reports(before);}
 @PutMapping("/api/admin/community/{id}") public void moderate(@PathVariable Long id,@RequestBody Moderate r){service.moderate(id,r.hidden());}
}
