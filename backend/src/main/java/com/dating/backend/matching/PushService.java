package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;import java.util.*;import java.net.URI;import java.security.MessageDigest;import java.nio.charset.StandardCharsets;
@Service @RequiredArgsConstructor public class PushService {
 private final EntityManager em;private final InboxService inbox;private final PushDelivery delivery;
 public record Subscription(String endpoint,Map<String,String> keys){}
 public record Config(boolean enabled,String publicKey){}
 public Config config(){return new Config(delivery.ready(),delivery.key());}
 private String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 @Transactional public void subscribe(String email,Subscription r){
  if(!delivery.ready())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"푸시 서버 설정이 필요합니다.");
  try{var uri=URI.create(r.endpoint());var host=uri.getHost();if(!"https".equals(uri.getScheme())||uri.getUserInfo()!=null||uri.getFragment()!=null||uri.getPort()!=-1||host==null||!(host.equals("fcm.googleapis.com")||host.equals("updates.push.services.mozilla.com")||host.endsWith(".notify.windows.com")||host.equals("web.push.apple.com")))throw new Exception();
   if(r.endpoint().length()>2000||Base64.getUrlDecoder().decode(r.keys().get("p256dh")).length!=65||Base64.getUrlDecoder().decode(r.keys().get("auth")).length!=16)throw new Exception();
  }catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"지원하지 않는 푸시 구독입니다.");}
  var u=inbox.user(email);em.lock(u,LockModeType.PESSIMISTIC_WRITE);String hash=hash(r.endpoint());var d=em.createQuery("select d from PushDevice d where d.endpointHash=:h",PushDevice.class).setParameter("h",hash).getResultStream().findFirst().orElse(null);
  if(d==null){long count=em.createQuery("select count(d) from PushDevice d where d.userId=:u",Long.class).setParameter("u",u.getId()).getSingleResult();if(count>=10)throw new ResponseStatusException(HttpStatus.CONFLICT,"등록 가능한 기기 수를 초과했습니다.");d=new PushDevice();d.setEndpointHash(hash);em.persist(d);}d.setUserId(u.getId());d.setEndpoint(r.endpoint());d.setP256dh(r.keys().get("p256dh"));d.setAuth(r.keys().get("auth"));
 }
 @Transactional public void unsubscribe(String email,String endpoint){em.createQuery("delete from PushDevice d where d.userId=:u and d.endpointHash=:h").setParameter("u",inbox.user(email).getId()).setParameter("h",hash(endpoint)).executeUpdate();}
 @Transactional public void deliver(){
  if(!delivery.ready())return;
  var pending=em.createQuery("select n from AppNotice n where n.pushed=false and n.pushAttempts<3 order by n.id",AppNotice.class).setMaxResults(10).getResultList();
  for(var n:pending){var preferences=em.find(AccountPreferences.class,n.getUserId());
   if(!AccountService.allowed(preferences,n.getEventKey())){n.setPushed(true);continue;}
   if(AccountService.quiet(preferences,java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul")).getHour())){n.setPushed(true);continue;}
   if(n.isSeen()){n.setPushed(true);continue;}var devices=em.createQuery("select d from PushDevice d where d.userId=:u",PushDevice.class).setParameter("u",n.getUserId()).getResultList();boolean success=true;
   for(var d:devices)try{int code=delivery.send(d);if(code==404||code==410)em.remove(d);else if(code<200||code>=300)success=false;}catch(Exception e){success=false;}
   n.setPushAttempts(n.getPushAttempts()+1);n.setPushed(success);
  }
 }
}
