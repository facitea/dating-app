package com.dating.backend.matching;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/matches/today")
@RequiredArgsConstructor
public class DailyMatchController {
    private final DailyMatchService service;
    @GetMapping("/extra") public DailyMatchService.ExtraQuote extra(Authentication a){return service.extraQuote(a.getName());}
    @PostMapping("/extra") public DailyMatchService.Card buyExtra(Authentication a,@RequestBody DailyMatchService.ExtraPurchase r){return service.buyExtra(a.getName(),r);}
    public record ReactionRequest(String reaction) {}
    @GetMapping
    public DailyMatchService.Deck today(Authentication authentication) {
        return service.today(authentication.getName());
    }
    @PostMapping("/{day}/{slot}/reveal")
    public DailyMatchService.Card reveal(Authentication authentication, @PathVariable LocalDate day, @PathVariable int slot) {
        return service.reveal(authentication.getName(), day, slot);
    }
    @PostMapping("/{day}/{slot}/reaction")
    public DailyMatchService.Card react(Authentication authentication, @PathVariable LocalDate day,
                                        @PathVariable int slot, @RequestBody ReactionRequest request) {
        return service.react(authentication.getName(), day, slot, request.reaction());
    }
}
