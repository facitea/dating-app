package com.dating.backend.matching;
import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import java.util.*;
@RestController @RequestMapping("/api/account") @RequiredArgsConstructor public class AccountController {
 private final AccountService accounts;
 @GetMapping("/settings") public AccountService.Settings get(Authentication a){return accounts.get(a.getName());}
 @PutMapping("/rest") public AccountService.Settings rest(Authentication a,@RequestBody Map<String,Boolean> r){return accounts.rest(a.getName(),Boolean.TRUE.equals(r.get("resting")));}
 @PutMapping("/notifications") public AccountService.Settings settings(Authentication a,@RequestBody AccountService.Preferences r){return accounts.preferences(a.getName(),r);}
 @PostMapping("/password") public void password(Authentication a,@RequestBody AccountService.PasswordChange r){accounts.password(a.getName(),r);}
 @PostMapping("/logout-all") public void logout(Authentication a){accounts.logoutAll(a.getName());}
 @PostMapping("/withdraw") public void withdraw(Authentication a,@RequestBody AccountService.Withdrawal r){accounts.withdraw(a.getName(),r);}
}
