package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import com.dating.backend.entity.User;import com.dating.backend.admin.AdminService;import com.google.firebase.FirebaseApp;import com.google.firebase.messaging.*;import java.util.*;
@Service @RequiredArgsConstructor public class NativePushService {
 private final EntityManager em;private final InboxService inbox;private final com.dating.backend.service.GoogleIdentityService firebase;
 public record Device(String token){}
 public boolean ready(){try{firebase.firebaseApp();return true;}catch(Exception e){return false;}}
 @Transactional public void subscribe(String email,Device r){if(!ready())throw AdminService.fail(503,"앱 푸시 서버 설정이 필요합니다.");if(r==null||r.token()==null||r.token().length()<20||r.token().length()>2048||!r.token().matches("[A-Za-z0-9_:-]+"))throw AdminService.fail(400,"잘못된 기기 정보입니다.");var u=inbox.user(email);em.lock(u,LockModeType.PESSIMISTIC_WRITE);String hash=AdminService.hash(r.token());var d=em.find(NativePushDevice.class,hash);if(d==null){long count=em.createQuery("select count(d) from NativePushDevice d where d.userId=:u",Long.class).setParameter("u",u.getId()).getSingleResult();if(count>=10)throw AdminService.fail(409,"등록 가능한 기기 수를 초과했습니다.");d=new NativePushDevice();d.setTokenHash(hash);d.setToken(r.token());em.persist(d);}d.setUserId(u.getId());d.setSessionVersion(u.getSessionVersion());d.setRegisteredAt(java.time.Instant.now());}
 @Transactional public void unsubscribe(String email,Device r){if(r==null||r.token()==null)return;em.createQuery("delete from NativePushDevice d where d.userId=:u and d.tokenHash=:h").setParameter("u",inbox.user(email).getId()).setParameter("h",AdminService.hash(r.token())).executeUpdate();}
 public boolean send(AppNotice n){if(!ready())return true;var u=em.find(User.class,n.getUserId());if(u==null||!"ACTIVE".equals(u.getStatus()))return true;boolean success=true;
 for(var d:em.createQuery("select d from NativePushDevice d where d.userId=:u",NativePushDevice.class).setParameter("u",n.getUserId()).getResultList()){
 if(d.getSessionVersion()!=u.getSessionVersion()){em.remove(d);continue;}if(n.getCreatedAt().isBefore(d.getRegisteredAt()))continue;
 try{var msg=Message.builder().setToken(d.getToken()).setNotification(Notification.builder().setTitle("그럼우리").setBody("새로운 소식이 도착했어요.").build()).putData("url","/notifications").setAndroidConfig(AndroidConfig.builder().setTtl(3600000).setNotification(AndroidNotification.builder().setChannelId("geureomuri-news").setTag("notice-"+n.getId()).build()).build()).build();FirebaseMessaging.getInstance(FirebaseApp.getInstance("dating-auth")).sendAsync(msg).get(10,java.util.concurrent.TimeUnit.SECONDS);}
 catch(Exception e){Throwable cause=e.getCause()==null?e:e.getCause();if(cause instanceof FirebaseMessagingException fe&&fe.getMessagingErrorCode()==MessagingErrorCode.UNREGISTERED)em.remove(d);else success=false;}
 }return success;
 }
}
