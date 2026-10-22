package com.dating.backend.matching;
import com.dating.backend.entity.*;import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;import java.time.*;import java.util.*;
@Service @RequiredArgsConstructor public class SanctionService {
 private final EntityManager em;private final InboxService inbox;
 @Transactional public void apply(Long userId,Long reportId,String action,Integer days,String reason){
  if(!Set.of("NONE","WARNING","TEMPORARY","PERMANENT").contains(action==null?"":action))throw bad("처리 방안을 선택해 주세요.");
  if("NONE".equals(action))return;
  if(reason==null||reason.isBlank()||reason.length()>1000)throw bad("회원에게 안내할 사유를 1~1,000자로 입력해 주세요.");
  if("TEMPORARY".equals(action)&&(days==null||days<1||days>365))throw bad("정지 기간은 1~365일입니다.");
  var u=em.find(User.class,userId,LockModeType.PESSIMISTIC_WRITE);if(u==null||"DELETED".equals(u.getStatus()))throw bad("탈퇴한 회원에게는 조치할 수 없습니다.");
  String label;
  if("WARNING".equals(action)){u.setWarningCount(u.getWarningCount()+1);label="경고";}
  else{
   if("PERMANENT".equals(u.getSanctionType()))throw bad("이미 영구 정지된 회원입니다.");
   if(u.getSanctionType()==null)u.setStatusBeforeSanction(u.getStatus());
   u.setSanctionType(action);u.setSanctionReason(reason);u.setStatus("SUSPENDED");
   if("PERMANENT".equals(action)){u.setSuspendedUntil(null);label="영구 정지";}
   else{Instant until=Instant.now().plus(Duration.ofDays(days));if(u.getSuspendedUntil()!=null&&u.getSuspendedUntil().isAfter(until))until=u.getSuspendedUntil();u.setSuspendedUntil(until);label=days+"일 정지 (종료: "+until.atZone(ZoneId.of("Asia/Seoul"))+ ")";}
   u.setSessionVersion(u.getSessionVersion()+1);
   em.createQuery("update RefreshToken t set t.revoked=true,t.revokedAt=:now where t.userId=:u").setParameter("now",LocalDateTime.now()).setParameter("u",userId).executeUpdate();
  }
  em.persist(AdminReviewHistory.builder().userId(userId).actionType("REPORT_"+action).detail("신고 #"+reportId+" · "+label+" · "+reason).actorLabel("관리자").build());
  inbox.add(userId,"SANCTION:"+reportId,"계정 조치 안내: "+label,reason,"/notifications");
 }
 @Transactional public void expire(Long id){var u=em.find(User.class,id,LockModeType.PESSIMISTIC_WRITE);if(u!=null&&"TEMPORARY".equals(u.getSanctionType())&&u.getSuspendedUntil()!=null&&!u.getSuspendedUntil().isAfter(Instant.now()))release(u,"기간 정지가 만료되었습니다.");}
 @Scheduled(fixedDelay=30000) @Transactional public void expireDue(){for(var u:em.createQuery("select u from User u where u.sanctionType='TEMPORARY' and u.suspendedUntil<=:now",User.class).setParameter("now",Instant.now()).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList())release(u,"기간 정지가 만료되었습니다.");}
 @Transactional public void lift(Long id,String note){var u=em.find(User.class,id,LockModeType.PESSIMISTIC_WRITE);if(u==null||u.getSanctionType()==null)throw bad("해제할 정지가 없습니다.");release(u,note);}
 private void release(User u,String note){if("SUSPENDED".equals(u.getStatus()))u.setStatus(u.getStatusBeforeSanction()==null?"ACTIVE":u.getStatusBeforeSanction());u.setSanctionType(null);u.setSuspendedUntil(null);u.setSanctionReason(null);u.setStatusBeforeSanction(null);em.persist(AdminReviewHistory.builder().userId(u.getId()).actionType("SANCTION_RELEASE").detail(note).actorLabel("운영 시스템").build());inbox.add(u.getId(),"RELEASE:"+UUID.randomUUID(),"정지 조치가 해제되었습니다",note,"/profile");}
 private ResponseStatusException bad(String s){return new ResponseStatusException(HttpStatus.CONFLICT,s);}
}
