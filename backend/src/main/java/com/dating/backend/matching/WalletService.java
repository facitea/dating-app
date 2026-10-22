package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;import java.util.*;
@Service @RequiredArgsConstructor public class WalletService {
 private final EntityManager em;private final InboxService inbox;
 @Value("${app.credits.test-topup:false}") private boolean testTopup;
 public record Wallet(int balance,boolean testMode,List<CreditEntry> entries){}
 public record Topup(int amount,String requestId){}
 @Transactional(readOnly=true) public Wallet get(String email){var u=inbox.user(email);var w=em.find(CreditWallet.class,u.getId());return new Wallet(w==null?0:w.getBalance(),testTopup,em.createQuery("select e from CreditEntry e where e.userId=:u order by e.id desc",CreditEntry.class).setParameter("u",u.getId()).setMaxResults(100).getResultList());}
 @Transactional public Wallet topup(String email,Topup r){
  if(!testTopup)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"테스트 충전이 비활성화되어 있습니다.");
  if(r==null||!List.of(100,300,500).contains(r.amount()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"충전 상품을 선택해 주세요.");
  try{UUID.fromString(r.requestId());}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"잘못된 충전 요청입니다.");}
  var u=inbox.user(email);em.lock(u,LockModeType.PESSIMISTIC_WRITE);String key="TEST_TOPUP:"+r.requestId();
  var existing=em.createQuery("select e from CreditEntry e where e.userId=:u and e.referenceKey=:k",CreditEntry.class).setParameter("u",u.getId()).setParameter("k",key).getResultStream().findFirst().orElse(null);
  if(existing!=null){if(existing.getAmount()!=r.amount())throw new ResponseStatusException(HttpStatus.CONFLICT);return get(email);}
  var w=em.find(CreditWallet.class,u.getId());if(w==null){w=new CreditWallet();w.setUserId(u.getId());em.persist(w);}if(w.getBalance()>100000-r.amount())throw new ResponseStatusException(HttpStatus.CONFLICT,"테스트 잔액 한도에 도달했습니다.");
  w.setBalance(w.getBalance()+r.amount());var entry=new CreditEntry();entry.setUserId(u.getId());entry.setAmount(r.amount());entry.setReferenceKey(key);em.persist(entry);
  inbox.add(u.getId(),key,"테스트 크레딧 충전 완료",r.amount()+"크레딧이 충전되었습니다. 실제 결제는 발생하지 않았습니다.","/credits");return get(email);
 }
}
