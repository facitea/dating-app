package com.dating.backend.matching;
import com.dating.backend.entity.*;import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;import java.util.*;
@Service @RequiredArgsConstructor public class ProfileModeration {
 private final EntityManager em;private final InboxService inbox;
 @Transactional public void beforeEdit(User u){
  em.lock(u,LockModeType.PESSIMISTIC_WRITE);if(!"ACTIVE".equals(u.getStatus()))return;
  if(em.find(ApprovedProfile.class,u.getId())==null){var saved=new ApprovedProfile();saved.setUserId(u.getId());saved.setNickname(u.getNickname());
   var p=em.createQuery("select p from UserProfile p where p.userId=:u",UserProfile.class).setParameter("u",u.getId()).getResultStream().findFirst().orElse(null);
   if(p!=null){saved.setRegion(p.getRegion());saved.setJob(p.getJob());saved.setMbti(p.getMbti());saved.setIntroduction(p.getIntroduction());saved.setPersonality(p.getPersonality());saved.setIdealType(p.getIdealType());saved.setSmoking(p.getSmokingStatus());saved.setDrinking(p.getDrinkingStatus());saved.setReligion(p.getReligion());}
   var v=em.createQuery("select v from UserVerification v where v.userId=:u",UserVerification.class).setParameter("u",u.getId()).getResultStream().findFirst().orElse(null);if(v!=null){saved.setBirthDate(v.getBirthDate());saved.setGender(v.getGender());}
   saved.setImages(new ArrayList<>(em.createQuery("select i.imageUrl from UserProfileImage i where i.userId=:u order by i.isMain desc,i.imageOrder",String.class).setParameter("u",u.getId()).getResultList()));em.persist(saved);
  }u.setProfileReviewStatus("PENDING");u.setProfileRevision(u.getProfileRevision()+1);u.setProfileReviewNote("변경 내용을 심사 중입니다. 승인 전에는 기존 프로필이 공개됩니다.");
 }
 @Transactional public void decide(Long id,long revision,boolean approve,String note){
  var u=em.find(User.class,id,LockModeType.PESSIMISTIC_WRITE);if(u==null||!"ACTIVE".equals(u.getStatus())||!"PENDING".equals(u.getProfileReviewStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"변경 심사 대기 상태가 아닙니다.");
  if(u.getProfileRevision()!=revision)throw new ResponseStatusException(HttpStatus.CONFLICT,"회원이 내용을 다시 수정했습니다. 새로고침 후 검토해 주세요.");
  if(note==null||note.isBlank()||note.length()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"심사 결과 안내를 1~1,000자로 입력해 주세요.");
  if(approve){if("HIGH_RISK".equals(u.getFraudReviewStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"계정 상태를 먼저 확인해 주세요.");if(em.createQuery("select count(i) from UserProfileImage i where i.userId=:u",Long.class).setParameter("u",id).getSingleResult()<1)throw new ResponseStatusException(HttpStatus.CONFLICT,"대표사진이 필요합니다.");var old=em.find(ApprovedProfile.class,id);if(old!=null)em.remove(old);u.setProfileReviewStatus("NONE");}else u.setProfileReviewStatus("REJECTED");
  u.setProfileReviewNote(note);inbox.add(id,"PROFILE:"+revision,"프로필 변경 "+(approve?"승인":"보완 요청"),note,"/profile/edit");
  em.persist(AdminReviewHistory.builder().userId(id).actionType(approve?"PROFILE_APPROVE":"PROFILE_REJECT").detail(note).actorLabel("관리자").build());
 }
}
