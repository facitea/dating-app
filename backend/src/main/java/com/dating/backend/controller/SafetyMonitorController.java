package com.dating.backend.controller;
import com.dating.backend.matching.*;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/admin/safety/monitor") @RequiredArgsConstructor public class SafetyMonitorController {
 private final SafetyEventService events;private final SanctionService sanctions;
 @GetMapping public List<SafetyEvent> list(@RequestParam(defaultValue="true") boolean open,@RequestParam(defaultValue="0") int page){return events.list(open,page);}
 public record Decision(String note,String action,Integer days){}
 @PutMapping("/{id}") @org.springframework.transaction.annotation.Transactional public void review(@PathVariable Long id,@RequestBody Decision r){var event=events.get(id);if(event.isReviewed())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"이미 검토한 기록입니다.");
  events.review(id,r.note());
  if(r.action()!=null&&!"NONE".equals(r.action())){if(event==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"최근 기록에서 선택해 주세요.");sanctions.apply(event.getUserId(),-id,r.action(),r.days(),r.note());}}
}
