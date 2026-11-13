package com.dating.backend.controller;
import com.dating.backend.matching.ProfileRewards;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;
@RestController @RequiredArgsConstructor @RequestMapping("/api/profile/completion") public class ProfileRewardsController {
 private final ProfileRewards rewards;
 @GetMapping public ProfileRewards.Progress get(Authentication a){return rewards.get(a.getName());}
 @PostMapping("/{percent}/claim") public ProfileRewards.Progress claim(Authentication a,@PathVariable int percent){return rewards.claim(a.getName(),percent);}
}
