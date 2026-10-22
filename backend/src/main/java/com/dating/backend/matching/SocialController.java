package com.dating.backend.matching;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/social") @RequiredArgsConstructor
public class SocialController {
    private final SocialService social;
    @GetMapping("/blocks") public List<SocialService.Person> blocks(Authentication a){return social.blocks(a.getName());}
    @DeleteMapping("/blocks/{id}") public void unblock(Authentication a,@PathVariable Long id){social.unblock(a.getName(),id);}
    @PostMapping("/matches/{id}/end") public void unmatch(Authentication a,@PathVariable Long id){social.unmatch(a.getName(),id);}
    @GetMapping("/profiles/{id}") public SocialService.PublicProfile profile(Authentication a,@PathVariable Long id){return social.profile(a.getName(),id);}
    @PostMapping("/blocks/{id}") public void block(Authentication a,@PathVariable Long id){social.block(a.getName(),id);}
    @DeleteMapping("/matches/{id}") public void hideMatch(Authentication a,@PathVariable Long id){social.hideMatch(a.getName(),id);}
    @DeleteMapping("/rooms/{id}") public void deleteRoom(Authentication a,@PathVariable Long id){social.deleteRoom(a.getName(),id);}
    private final NotificationService notifications;
    @GetMapping("/notifications") public NotificationService.Badges notifications(Authentication a) { return notifications.get(a.getName()); }
    @PostMapping("/notifications/seen") public void seen(Authentication a,@RequestBody NotificationService.Seen r) { notifications.acknowledge(a.getName(),r); }
    private final MatchPreferenceService preferences;
    @GetMapping("/preferences") public MatchPreferenceService.Settings preferences(Authentication a) { return preferences.get(a.getName()); }
    @PutMapping("/preferences") public MatchPreferenceService.Settings save(Authentication a,@RequestBody MatchPreferenceService.Conditions c) { return preferences.save(a.getName(),c); }
    @GetMapping("/matches") public SocialService.Overview matches(Authentication a) { return social.overview(a.getName()); }
    @PostMapping("/likes/{id}") public SocialService.Connection like(Authentication a,@PathVariable Long id) { return social.like(a.getName(),id); }
    @PostMapping("/likes/{id}/dismiss") public void dismiss(Authentication a,@PathVariable Long id) { social.dismiss(a.getName(),id); }
    @GetMapping("/contact/{id}") public SocialService.Quote quote(Authentication a,@PathVariable Long id) { return social.quote(a.getName(),id); }
    @PostMapping("/direct") public SocialService.OpenResult direct(Authentication a,@RequestBody SocialService.DirectRequest r) { return social.direct(a.getName(),r); }
    @GetMapping("/rooms") public List<SocialService.Room> rooms(Authentication a) { return social.rooms(a.getName()); }
    @GetMapping("/rooms/{id}/messages") public SocialService.Messages messages(Authentication a,@PathVariable Long id,@RequestParam(defaultValue="0") long after,@RequestParam(defaultValue="0") long before) { return social.messages(a.getName(),id,after,before); }
    @PostMapping("/rooms/{id}/messages") public SocialService.Message send(Authentication a,@PathVariable Long id,@RequestBody SocialService.SendRequest r) { return social.send(a.getName(),id,r); }
}
