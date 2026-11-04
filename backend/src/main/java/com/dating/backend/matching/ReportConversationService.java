package com.dating.backend.matching;
import com.dating.backend.entity.UserReport;import com.dating.backend.admin.AdminService;import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.*;import java.time.*;
@Service @RequiredArgsConstructor public class ReportConversationService {
 private final EntityManager em;private final AdminService admins;
 public String validate(Long reporter,Long target,List<Long> ids){
  if(ids==null||ids.isEmpty())return null;if(ids.size()>10||ids.stream().anyMatch(Objects::isNull))throw AdminService.fail(400,"신고할 메시지는 최대 10개까지 선택해 주세요.");
  var room=room(reporter,target);if(room==null)throw AdminService.fail(400,"대화를 찾을 수 없습니다.");
  var unique=new LinkedHashSet<>(ids);for(Long id:unique){var m=em.find(ChatMessage.class,id);if(m==null||!m.getRoomId().equals(room.getId())||!m.getSenderId().equals(target))throw AdminService.fail(400,"상대가 보낸 대화만 신고할 수 있습니다.");}
  return unique.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
 }
 private ChatRoom room(Long a,Long b){return em.createQuery("select r from ChatRoom r where r.lowId=:a and r.highId=:b",ChatRoom.class).setParameter("a",Math.min(a,b)).setParameter("b",Math.max(a,b)).getResultStream().findFirst().orElse(null);}
 public record Line(Long id,Long senderId,String text,Instant createdAt,boolean selected){}
 @Transactional public List<Line> view(Long reportId){
  AdminService.requireSession();var a=(AdminService.Principal)org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();if(!Set.of("OWNER","SUPPORT","REVIEWER").contains(a.role()))throw AdminService.fail(403,"신고 열람 권한이 없습니다.");
  var report=em.find(UserReport.class,reportId);if(report==null)throw AdminService.fail(404,"신고를 찾을 수 없습니다.");admins.audit(a.id(),a.username(),"VIEW_REPORT_CONVERSATION:"+reportId,200);
  var room=room(report.getReporterUserId(),report.getReportedUserId());if(room==null)return List.of();
  Set<Long> selected=new HashSet<>();if(report.getSelectedMessageIds()!=null)for(String id:report.getSelectedMessageIds().split(","))selected.add(Long.valueOf(id));
  var found=new TreeMap<Long,ChatMessage>();
  if(selected.isEmpty()) em.createQuery("select m from ChatMessage m where m.roomId=:r and m.createdAt<=:t order by m.id desc",ChatMessage.class).setParameter("r",room.getId()).setParameter("t",report.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()).setMaxResults(100).getResultList().forEach(m->found.put(m.getId(),m));
  else for(Long id:selected)for(String comparison:List.of("<=",">"))em.createQuery("select m from ChatMessage m where m.roomId=:r and m.id"+comparison+":id order by m.id "+(comparison.equals("<=")?"desc":"asc"),ChatMessage.class).setParameter("r",room.getId()).setParameter("id",id).setMaxResults(comparison.equals("<=")?6:5).getResultList().forEach(m->found.put(m.getId(),m));
  return found.values().stream().map(m->new Line(m.getId(),m.getSenderId(),m.getBody(),m.getCreatedAt(),selected.contains(m.getId()))).toList();
 }
}
