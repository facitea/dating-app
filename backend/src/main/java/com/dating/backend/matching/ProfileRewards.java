package com.dating.backend.matching;
import com.dating.backend.entity.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service @RequiredArgsConstructor public class ProfileRewards {
 private final EntityManager em;private final InboxService inbox;private final TraitService traits;
 public record Item(String label,boolean done,String link){}
 public record Step(int percent,int coins,boolean claimed,boolean available){}
 public record Progress(int percent,List<Item> items,List<Step> steps){}
 private boolean filled(String s){return s!=null&&!s.isBlank();}
 @Transactional(readOnly=true) public Progress get(String email){var u=em.createQuery("select u from User u where u.email=:e and u.status<>'DELETED'",User.class).setParameter("e",email).getResultStream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));return progress(u);}
 private Progress progress(User u){
  var p=em.createQuery("select p from UserProfile p where p.userId=:u",UserProfile.class).setParameter("u",u.getId()).getResultStream().findFirst().orElse(null);
  long photos=em.createQuery("select count(p) from UserProfileImage p where p.userId=:u",Long.class).setParameter("u",u.getId()).getSingleResult();
  var answers=traits.read(p==null?null:p.getTraitsJson()).answers();
  var items=List.of(new Item("프로필 사진 1장",photos>=1,"/profile/photos"),new Item("프로필 사진 2장",photos>=2,"/profile/photos"),new Item("자기소개 20자 이상",p!=null&&p.getIntroduction()!=null&&p.getIntroduction().strip().length()>=20,"/profile/edit"),new Item("거주 지역",p!=null&&filled(p.getRegion()),"/profile/edit"),new Item("직업",p!=null&&filled(p.getJob()),"/profile/edit"),new Item("MBTI",p!=null&&filled(p.getMbti()),"/profile/edit"),new Item("흡연·음주·종교",p!=null&&filled(p.getSmokingStatus())&&filled(p.getDrinkingStatus())&&filled(p.getReligion()),"/profile/edit"),new Item("성향 키워드",!answers.getOrDefault("personality",List.of()).isEmpty(),"/profile/edit"),new Item("관심사·취미",!answers.getOrDefault("hobbies",List.of()).isEmpty(),"/profile/edit"),new Item("소통·연애·생활 등 추가 성향",answers.entrySet().stream().anyMatch(e->!Set.of("personality","hobbies").contains(e.getKey())&&e.getValue()!=null&&!e.getValue().isEmpty()),"/profile/edit"));
  int score=(int)items.stream().filter(Item::done).count()*10;
  var keys=em.createQuery("select e.referenceKey from CreditEntry e where e.userId=:u and e.referenceKey like 'PROFILE_REWARD:%'",String.class).setParameter("u",u.getId()).getResultList();
  return new Progress(score,items,List.of(25,50,75,100).stream().map(n->new Step(n,2,keys.contains("PROFILE_REWARD:"+n),"ACTIVE".equals(u.getStatus())&&score>=n&&!keys.contains("PROFILE_REWARD:"+n))).toList());
 }
 @Transactional public Progress claim(String email,int percent){
  if(!List.of(25,50,75,100).contains(percent))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"잘못된 보상 단계입니다.");
  var u=inbox.user(email);em.lock(u,LockModeType.PESSIMISTIC_WRITE);var state=progress(u);var step=state.steps().stream().filter(s->s.percent()==percent).findFirst().orElseThrow();
  if(step.claimed())return state;
  if(!step.available())throw new ResponseStatusException(HttpStatus.CONFLICT,"아직 해당 완성도에 도달하지 않았어요.");
  var wallet=em.find(CreditWallet.class,u.getId(),LockModeType.PESSIMISTIC_WRITE);if(wallet==null){wallet=new CreditWallet();wallet.setUserId(u.getId());em.persist(wallet);}
  wallet.setBalance(Math.addExact(wallet.getBalance(),2));var entry=new CreditEntry();entry.setUserId(u.getId());entry.setAmount(2);entry.setReferenceKey("PROFILE_REWARD:"+percent);entry.setMessage("프로필 완성도 "+percent+"% 달성 보상");em.persist(entry);return progress(u);
 }
}
