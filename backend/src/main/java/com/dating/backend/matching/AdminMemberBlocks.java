package com.dating.backend.matching;
import com.dating.backend.entity.User;import com.dating.backend.admin.AdminService;import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.*;
@Service @RequiredArgsConstructor public class AdminMemberBlocks {
 private final EntityManager em;private final AdminService admins;
 public record Release(String reason){}
 @Transactional public void release(Long owner,Long target,Release request){
  AdminService.requireSession();var a=(AdminService.Principal)org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();if(!Set.of("OWNER","SUPPORT").contains(a.role()))throw AdminService.fail(403,"민원 처리 권한이 필요합니다.");
  if(owner.equals(target)||request==null||request.reason()==null||request.reason().isBlank()||request.reason().length()>200)throw AdminService.fail(400,"해제 사유를 200자 이내로 입력해 주세요.");
  for(Long id:List.of(Math.min(owner,target),Math.max(owner,target)))if(em.find(User.class,id,LockModeType.PESSIMISTIC_WRITE)==null)throw AdminService.fail(404,"회원을 찾을 수 없습니다.");
  var v=em.createQuery("select v from MemberVisibility v where v.ownerId=:o and v.targetId=:t",MemberVisibility.class).setParameter("o",owner).setParameter("t",target).getResultStream().findFirst().orElse(null);
  if(v!=null&&v.isBlocked()){var u=em.find(User.class,target);long count=em.createQuery("select count(v) from MemberVisibility v where v.targetId=:t and v.blocked=true",Long.class).setParameter("t",target).getSingleResult();u.setTotalBlockCount(Math.max(u.getTotalBlockCount(),count));v.setBlocked(false);}
  for(var report:em.createQuery("select r from UserReport r where r.reporterUserId=:o and r.reportedUserId=:t and r.contactReleasedAt is null",com.dating.backend.entity.UserReport.class).setParameter("o",owner).setParameter("t",target).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList()){report.setContactReleasedAt(java.time.Instant.now());report.setContactReleasedBy(a.id());report.setContactReleaseReason(request.reason().trim());}
  admins.audit(a.id(),a.username(),"RELEASE_MEMBER_BLOCKS:"+owner+":"+target+":"+request.reason().trim(),200);
 }
 public record Item(Long id,String nickname,String status,boolean directlyBlocked,boolean reported){}
 public record Page(long total,List<Item> items){}
 @Transactional public Page list(Long owner,int page){AdminService.requireSession();var a=(AdminService.Principal)org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();if(!Set.of("OWNER","SUPPORT").contains(a.role()))throw AdminService.fail(403,"민원 처리 권한이 필요합니다.");if(page<0||page>10000)throw AdminService.fail(400,"페이지를 확인해 주세요.");if(em.find(User.class,owner)==null)throw AdminService.fail(404,"회원을 찾을 수 없습니다.");
 String direct="exists (select v.id from MemberVisibility v where v.ownerId=:owner and v.targetId=u.id and v.blocked=true)",reported="exists (select r.id from UserReport r where r.contactReleasedAt is null and r.reporterUserId=:owner and r.reportedUserId=u.id)";String filter=" from User u where ("+direct+" or "+reported+")";
 long total=em.createQuery("select count(u)"+filter,Long.class).setParameter("owner",owner).getSingleResult();
 var users=em.createQuery("select u"+filter+" order by u.id desc",User.class).setParameter("owner",owner).setFirstResult(page*30).setMaxResults(30).getResultList();
 var items=users.stream().map(u->new Item(u.getId(),u.getNickname(),u.getStatus(),em.createQuery("select count(v) from MemberVisibility v where v.ownerId=:owner and v.targetId=:id and v.blocked=true",Long.class).setParameter("owner",owner).setParameter("id",u.getId()).getSingleResult()>0,em.createQuery("select count(r) from UserReport r where r.contactReleasedAt is null and r.reporterUserId=:owner and r.reportedUserId=:id",Long.class).setParameter("owner",owner).setParameter("id",u.getId()).getSingleResult()>0)).toList();
 admins.audit(a.id(),a.username(),"VIEW_MEMBER_BLOCKS:"+owner+":PAGE:"+page,200);return new Page(total,items);
 }
}
