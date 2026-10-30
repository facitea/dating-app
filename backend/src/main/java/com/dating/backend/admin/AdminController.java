package com.dating.backend.admin;
import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import jakarta.servlet.http.*;
import org.springframework.http.*;import org.springframework.beans.factory.annotation.Value;import java.util.*;
import com.dating.backend.service.AbuseProtectionService;import com.dating.backend.service.AbuseRequestContext;
@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor public class AdminController {
 private final AdminService admins;private final AbuseProtectionService abuse;
 @Value("${app.auth.refresh-cookie-secure:false}") private boolean secure;
 public record Credentials(String username,String password){}
 public record NewAccount(String username,String password,String role){}
 public record Change(String role,boolean enabled,String password){}
 public record Password(String currentPassword,String newPassword){}
 private void cookie(HttpServletResponse r,String token,long age){r.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("dating_admin",token).httpOnly(true).secure(secure).sameSite("Strict").path("/").maxAge(age).build().toString());r.setHeader("Cache-Control","no-store");}
 @PostMapping("/session/login") public Map<String,Object> login(@RequestBody Credentials c,HttpServletRequest request,HttpServletResponse response){abuse.protectLogin(c.username(),new AbuseRequestContext(request.getRemoteAddr(),request.getHeader("User-Agent")));var result=admins.login(c.username(),c.password());cookie(response,result.token(),28800);return Map.of("account",result.account(),"csrf",result.csrf());}
 @GetMapping("/session") public AdminService.Principal me(@RequestAttribute("admin") AdminService.Principal p){return p;}
 @PostMapping("/session/logout") public void logout(@CookieValue(name="dating_admin",required=false)String token,HttpServletResponse response){admins.logout(token);cookie(response,"",0);}
 @PostMapping("/session/password") public void password(@RequestAttribute("admin") AdminService.Principal p,@RequestBody Password r,HttpServletResponse response){admins.changePassword(p,r.currentPassword(),r.newPassword());cookie(response,"",0);}
 @GetMapping("/accounts") public List<AdminService.Account> list(){return admins.accounts();}
 @PostMapping("/accounts") public AdminService.Account create(@RequestBody NewAccount r){return admins.create(r.username(),r.password(),r.role());}
 @PutMapping("/accounts/{id}") public void update(@PathVariable Long id,@RequestBody Change r){admins.update(id,r.role(),r.enabled(),r.password());}
 @GetMapping("/audit") public List<AdminAudit> audit(){return admins.audits();}
}
