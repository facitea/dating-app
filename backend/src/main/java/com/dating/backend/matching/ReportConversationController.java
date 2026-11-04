package com.dating.backend.matching;
import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequiredArgsConstructor public class ReportConversationController {
 private final ReportConversationService service;
 @GetMapping("/api/admin/safety/reports/{id}/conversation") public List<ReportConversationService.Line> view(@PathVariable Long id){return service.view(id);}
}
