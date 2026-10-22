package com.dating.backend.matching;
import com.dating.backend.entity.User;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service @RequiredArgsConstructor
public class NotificationService {
    private final EntityManager em;
    private final DailyMatchSchedule schedule;
    private final SocialService social;
    public record Badges(boolean today,long matches,long chats) {}
    public record Seen(List<String> keys) {}
    static String todayKey(DailyMatchSchedule.Window w) { return "TODAY:"+(w.now().isBefore(w.evening())?w.morning():w.evening()); }
    @Transactional(readOnly=true) public Badges get(String email) {
        var user=owner(email,false);
        var seen=seen(user.getId());var events=events(email);
        long matches=events.stream().filter(k->!k.startsWith("TODAY:")&&!seen.contains(k)).count();
        long unread=social.rooms(email).stream().mapToLong(SocialService.Room::unread).sum();
        return new Badges(!seen.contains(todayKey(schedule.current())),matches,unread);
    }
    @Transactional public void acknowledge(String email,Seen request) {
        if(request==null||request.keys()==null||request.keys().size()>200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"확인할 알림을 선택해 주세요.");
        var user=owner(email,true);var allowed=events(email);var seen=seen(user.getId());
        // Only acknowledge the exact events rendered by the client, never events arriving afterwards.
        for(String key:request.keys()) if(allowed.contains(key)&&seen.add(key)) {
            var receipt=new NotificationReceipt();receipt.setUserId(user.getId());receipt.setEventKey(key);em.persist(receipt);
        }
    }
    private Set<String> events(String email) {
        Set<String> events=new HashSet<>();events.add(todayKey(schedule.current()));
        var overview=social.overview(email);
        overview.received().forEach(c->events.add(c.eventKey()));
        overview.matched().forEach(c->events.add(c.eventKey()));
        events.remove(null);return events;
    }
    private Set<String> seen(Long userId) {
        return new HashSet<>(em.createQuery("select n.eventKey from NotificationReceipt n where n.userId=:id",String.class).setParameter("id",userId).getResultList());
    }
    private User owner(String email,boolean lock) {
        var q=em.createQuery("select u from User u where u.email=:email",User.class).setParameter("email",email);
        if(lock)q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        var user=q.getResultStream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if(!"ACTIVE".equals(user.getStatus()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }
}
