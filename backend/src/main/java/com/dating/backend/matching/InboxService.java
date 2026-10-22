package com.dating.backend.matching;
import com.dating.backend.entity.User;import jakarta.persistence.*;import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;import java.util.*;
@Service @RequiredArgsConstructor public class InboxService {
 private final EntityManager em;
 @Transactional public void add(Long userId,String key,String title,String body,String link){
  if(em.createQuery("select count(n) from AppNotice n where n.userId=:u and n.eventKey=:k",Long.class).setParameter("u",userId).setParameter("k",key).getSingleResult()>0)return;
  var n=new AppNotice();n.setUserId(userId);n.setEventKey(key);n.setTitle(title);n.setBody(body);n.setLink(link);em.persist(n);
 }
 @Transactional(readOnly=true) public List<AppNotice> list(String email,long before){var u=user(email);var q=em.createQuery("select n from AppNotice n where n.userId=:u"+(before>0?" and n.id<:before":"")+" order by n.id desc",AppNotice.class).setParameter("u",u.getId());if(before>0)q.setParameter("before",before);return q.setMaxResults(50).getResultList();}
 @Transactional(readOnly=true) public long unread(String email){return em.createQuery("select count(n) from AppNotice n where n.userId=:u and n.seen=false",Long.class).setParameter("u",user(email).getId()).getSingleResult();}
 @Transactional public void read(String email,List<Long> ids){if(ids==null||ids.size()>200)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);if(ids.isEmpty())return;em.createQuery("update AppNotice n set n.seen=true where n.userId=:u and n.id in :ids").setParameter("u",user(email).getId()).setParameter("ids",ids).executeUpdate();}
 User user(String email){return em.createQuery("select u from User u where u.email=:e and u.status='ACTIVE'",User.class).setParameter("e",email).getResultStream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));}
}
