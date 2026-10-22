package com.dating.backend.matching;
import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import java.util.*;
@RestController @RequestMapping("/api/social") @RequiredArgsConstructor public class EngagementController {
 private final InboxService inbox;private final WalletService wallet;private final PushService push;
 @GetMapping("/inbox") public List<AppNotice> list(Authentication a,@RequestParam(defaultValue="0")long before){return inbox.list(a.getName(),before);}
 @GetMapping("/inbox/unread") public long unread(Authentication a){return inbox.unread(a.getName());}
 @PostMapping("/inbox/read") public void read(Authentication a,@RequestBody List<Long> ids){inbox.read(a.getName(),ids);}
 @GetMapping("/wallet") public WalletService.Wallet wallet(Authentication a){return wallet.get(a.getName());}
 @PostMapping("/wallet/test-topup") public WalletService.Wallet topup(Authentication a,@RequestBody WalletService.Topup r){return wallet.topup(a.getName(),r);}
 @GetMapping("/push/config") public PushService.Config config(){return push.config();}
 @PostMapping("/push/subscription") public void subscribe(Authentication a,@RequestBody PushService.Subscription r){push.subscribe(a.getName(),r);}
 @DeleteMapping("/push/subscription") public void unsubscribe(Authentication a,@RequestBody Map<String,String> r){push.unsubscribe(a.getName(),r.getOrDefault("endpoint",""));}
}
