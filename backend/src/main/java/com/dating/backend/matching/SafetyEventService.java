package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import java.time.*;import java.util.*;
@Service @RequiredArgsConstructor public class SafetyEventService {
 private final EntityManager em;
 // No FK to locked member/message rows: rejected actions survive outer transaction rollback.
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void record(Long user,Long target,Long room,String type,String detail){record(user,target,room,type,detail,null);}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void record(Long user,Long target,Long room,String type,String detail,Integer score){
  if(em.createQuery("select count(e) from SafetyEvent e where e.userId=:u and e.type=:t and e.createdAt>:since",Long.class).setParameter("u",user).setParameter("t",type).setParameter("since",Instant.now().minusSeconds(60)).getSingleResult()>0)return;
  var e=new SafetyEvent();e.setUserId(user);e.setTargetId(target);e.setRoomId(room);e.setType(type);e.setScore(score);e.setDetail(detail.substring(0,Math.min(detail.length(),500)));em.persist(e);
 }
 @Transactional(readOnly=true) public List<SafetyEvent> list(boolean open,int page){return em.createQuery("select e from SafetyEvent e"+(open?" where e.reviewed=false":"")+" order by e.id desc",SafetyEvent.class).setFirstResult(Math.max(0,Math.min(page,10000))*50).setMaxResults(50).getResultList();}
 @Transactional public SafetyEvent get(Long id){var e=em.find(SafetyEvent.class,id,LockModeType.PESSIMISTIC_WRITE);if(e==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);return e;}
 @Transactional public void review(Long id,String note){var e=em.find(SafetyEvent.class,id,LockModeType.PESSIMISTIC_WRITE);if(e==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);if(note==null||note.isBlank()||note.length()>1000)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"검토 메모를 입력해 주세요.");e.setReviewed(true);e.setReviewNote(note);}
}
