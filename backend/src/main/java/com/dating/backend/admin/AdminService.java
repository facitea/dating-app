package com.dating.backend.admin;
import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;import java.util.*;import java.nio.charset.StandardCharsets;import java.security.*;

@Service @RequiredArgsConstructor public class AdminService {
 private final EntityManager em; private final PasswordEncoder encoder;
 public record Account(Long id,String username,String role,boolean enabled,Instant createdAt){}
 public record Login(String token,String csrf,Account account){}
 public record Principal(Long id,String username,String role,String csrf){}
 public static void requireSession(){var a=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();if(a==null||!(a.getPrincipal() instanceof Principal))throw fail(401,"관리자 로그인이 필요합니다.");}
 public static ResponseStatusException fail(int code,String text){return new ResponseStatusException(HttpStatus.valueOf(code),text);}
 public static String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private String random(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 private Account dto(AdminAccount a){return new Account(a.getId(),a.getUsername(),a.getRole(),a.isEnabled(),a.getCreatedAt());}
 public static void passwordPolicy(String p){if(p==null||p.length()<12||p.getBytes(StandardCharsets.UTF_8).length>72)throw fail(400,"비밀번호는 12자 이상, UTF-8 72바이트 이하로 입력해 주세요.");}
 @Transactional(noRollbackFor=ResponseStatusException.class) public Login login(String username,String password){
  String name=username==null?"":username.trim().toLowerCase(Locale.ROOT);
  var a=em.createQuery("select a from AdminAccount a where a.username=:n",AdminAccount.class).setParameter("n",name).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst().orElse(null);
  if(a==null)throw fail(401,"아이디 또는 비밀번호를 확인해 주세요.");
  if(a.getLockedUntil()!=null&&a.getLockedUntil().isAfter(Instant.now()))throw fail(429,"로그인 시도가 많습니다. 15분 후 다시 시도해 주세요.");
  if(!a.isEnabled()||password==null||password.getBytes(StandardCharsets.UTF_8).length>72||!encoder.matches(password,a.getPasswordHash())){a.setFailures(a.getFailures()+1);if(a.getFailures()>=5){a.setLockedUntil(Instant.now().plusSeconds(900));a.setFailures(0);}audit(a.getId(),a.getUsername(),"LOGIN_FAILED",401);throw fail(401,"아이디 또는 비밀번호를 확인해 주세요.");}
  a.setFailures(0);a.setLockedUntil(null);var s=new AdminSession();String token=random();s.setTokenHash(hash(token));s.setAdminId(a.getId());s.setCsrf(random());s.setExpiresAt(Instant.now().plusSeconds(28800));em.persist(s);audit(a.getId(),a.getUsername(),"LOGIN",200);return new Login(token,s.getCsrf(),dto(a));
 }
 @Transactional(readOnly=true) public Principal authenticate(String token){if(token==null||token.length()>128)return null;var s=em.createQuery("select s from AdminSession s where s.tokenHash=:hash",AdminSession.class).setParameter("hash",hash(token)).getResultStream().findFirst().orElse(null);if(s==null||!s.getExpiresAt().isAfter(Instant.now()))return null;var a=em.find(AdminAccount.class,s.getAdminId());return a==null||!a.isEnabled()?null:new Principal(a.getId(),a.getUsername(),a.getRole(),s.getCsrf());}
 @Transactional public void logout(String token){if(token!=null){var s=em.find(AdminSession.class,hash(token));if(s!=null)em.remove(s);}}
 @Transactional(readOnly=true) public List<Account> accounts(){return em.createQuery("select a from AdminAccount a order by a.id",AdminAccount.class).getResultList().stream().map(this::dto).toList();}
 @Transactional public Account create(String username,String password,String role){passwordPolicy(password);if(username==null||!username.matches("[A-Za-z0-9_.@-]{3,80}"))throw fail(400,"아이디는 영문·숫자·._@- 3~80자로 입력해 주세요.");checkRole(role);String name=username.toLowerCase(Locale.ROOT);if(em.createQuery("select count(a) from AdminAccount a where a.username=:n",Long.class).setParameter("n",name).getSingleResult()>0)throw fail(409,"이미 사용 중인 관리자 아이디입니다.");var a=new AdminAccount();a.setUsername(name);a.setPasswordHash(encoder.encode(password));a.setRole(role);em.persist(a);return dto(a);}
 private void checkRole(String role){if(!Set.of("OWNER","REVIEWER","SUPPORT","READ_ONLY").contains(role==null?"":role))throw fail(400,"권한을 확인해 주세요.");}
 @Transactional public void update(Long id,String role,boolean enabled,String password){checkRole(role);var all=em.createQuery("select a from AdminAccount a order by a.id",AdminAccount.class).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();var a=all.stream().filter(x->x.getId().equals(id)).findFirst().orElseThrow(()->fail(404,"관리자를 찾을 수 없습니다."));if(a.isEnabled()&&"OWNER".equals(a.getRole())&&(!enabled||!"OWNER".equals(role))&&all.stream().filter(x->x.isEnabled()&&"OWNER".equals(x.getRole())).count()<=1)throw fail(409,"최소 한 명의 활성 최고관리자가 필요합니다.");if(password!=null&&!password.isBlank()){passwordPolicy(password);a.setPasswordHash(encoder.encode(password));a.setFailures(0);a.setLockedUntil(null);}a.setRole(role);a.setEnabled(enabled);em.createQuery("delete from AdminSession s where s.adminId=:id").setParameter("id",id).executeUpdate();}
 @Transactional public void changePassword(Principal p,String oldPassword,String newPassword){var a=em.find(AdminAccount.class,p.id(),LockModeType.PESSIMISTIC_WRITE);if(oldPassword==null||!encoder.matches(oldPassword,a.getPasswordHash()))throw fail(400,"현재 비밀번호를 확인해 주세요.");passwordPolicy(newPassword);a.setPasswordHash(encoder.encode(newPassword));em.createQuery("delete from AdminSession s where s.adminId=:id").setParameter("id",p.id()).executeUpdate();}
 @Transactional public void bootstrap(String name,String password){if(em.createQuery("select count(a) from AdminAccount a",Long.class).getSingleResult()==0&&name!=null&&!name.isBlank())create(name,password,"OWNER");}
 @Transactional public void audit(Long id,String name,String action,int status){var a=new AdminAudit();a.setAdminId(id);a.setUsername(name);a.setAction(action.substring(0,Math.min(300,action.length())));a.setStatus(status);em.persist(a);}
 @Transactional(readOnly=true) public List<AdminAudit> audits(){return em.createQuery("select a from AdminAudit a order by a.id desc",AdminAudit.class).setMaxResults(200).getResultList();}
}
