package com.dating.backend.matching;
import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import com.dating.backend.service.AdminReviewService;import java.util.*;
@RestController @RequestMapping("/api/admin/console") @RequiredArgsConstructor public class AdminConsoleController {
 private final AdminReviewService auth;private final AdminConsoleService console;private final ProfileModeration moderation;private final CompensationService compensation;private final AdminMemberBlocks blocks;
 @GetMapping("/members/{id}/blocks") public AdminMemberBlocks.Page blocks(@PathVariable Long id,@RequestParam(defaultValue="0")int page){return blocks.list(id,page);}
 @PostMapping("/members/{id}/compensation") public CompensationService.Result grant(@PathVariable Long id,@RequestBody CompensationService.Grant r){return compensation.grant(id,r);}
 @PostMapping("/members/{id}/blocks/{target}/release") public void release(@PathVariable Long id,@PathVariable Long target,@RequestBody AdminMemberBlocks.Release request){blocks.release(id,target,request);}
 public record Decision(long revision,boolean approve,String note){}
 @GetMapping("/summary") public Map<String,Long> summary(@RequestHeader(value="X-Admin-Key",required=false)String key){auth.validateAdminKey(key);return console.summary();}
 @GetMapping("/members") public AdminConsoleService.Page list(@RequestHeader(value="X-Admin-Key",required=false)String key,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="ALL")String status,@RequestParam(defaultValue="ALL")String review,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="NEWEST")String sort){auth.validateAdminKey(key);return console.members(q,status,review,page,sort);}
 @GetMapping("/members/{id}") public AdminConsoleService.Detail detail(@RequestHeader(value="X-Admin-Key",required=false)String key,@PathVariable Long id){auth.validateAdminKey(key);return console.detail(id);}
 @PostMapping("/members/{id}/review") public void decide(@RequestHeader(value="X-Admin-Key",required=false)String key,@PathVariable Long id,@RequestBody Decision r){auth.validateAdminKey(key);moderation.decide(id,r.revision(),r.approve(),r.note());}
}
