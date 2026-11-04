package com.dating.backend.matching;
import com.dating.backend.entity.User;import com.dating.backend.admin.AdminService;import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.*;
@Service @RequiredArgsConstructor public class CompensationService {
 private final EntityManager em;private final InboxService inbox;
 public record Grant(int amount,String message,String requestId){}
 public record Result(int balance,Long entryId){}
 @Transactional public Result grant(Long id,Grant r){
  AdminService.requireSession();var principal=(AdminService.Principal)org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  if(!Set.of("OWNER","SUPPORT").contains(principal.role()))throw AdminService.fail(403,"보상 지급 권한이 없습니다.");
  if(r==null||r.amount()<1||r.amount()>10000||r.message()==null||r.message().isBlank()||r.message().length()>1000)throw AdminService.fail(400,"1~10,000크레딧과 안내 메시지를 입력해 주세요.");
  try{UUID.fromString(r.requestId());}catch(Exception e){throw AdminService.fail(400,"지급 요청을 다시 확인해 주세요.");}
  var u=em.find(User.class,id,LockModeType.PESSIMISTIC_WRITE);if(u==null||"DELETED".equals(u.getStatus()))throw AdminService.fail(404,"지급할 회원을 찾을 수 없습니다.");
  String key="COMPENSATION:"+r.requestId();var old=em.createQuery("select e from CreditEntry e where e.userId=:u and e.referenceKey=:k",CreditEntry.class).setParameter("u",id).setParameter("k",key).getResultStream().findFirst().orElse(null);
  var w=em.find(CreditWallet.class,id,LockModeType.PESSIMISTIC_WRITE);
  if(old!=null){if(old.getAmount()!=r.amount()||!Objects.equals(old.getMessage(),r.message().trim()))throw AdminService.fail(409,"이미 처리된 요청 내용과 다릅니다.");return new Result(w==null?0:w.getBalance(),old.getId());}
  if(w==null){w=new CreditWallet();w.setUserId(id);em.persist(w);}if(w.getBalance()>Integer.MAX_VALUE-r.amount())throw AdminService.fail(409,"잔액 한도를 초과했습니다.");
  w.setBalance(w.getBalance()+r.amount());var e=new CreditEntry();e.setUserId(id);e.setAmount(r.amount());e.setMessage(r.message().trim());e.setAdminId(principal.id());e.setReferenceKey(key);em.persist(e);
  inbox.add(id,key,"서비스 보상 "+r.amount()+"크레딧이 지급됐어요",r.message().trim(),"/credits");return new Result(w.getBalance(),e.getId());
 }
}
