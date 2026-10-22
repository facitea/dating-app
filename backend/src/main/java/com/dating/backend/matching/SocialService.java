package com.dating.backend.matching;
import com.dating.backend.entity.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class SocialService {
    private final EntityManager em;
    private final InboxService inbox;
    private final DailyMatchSchedule schedule;
    @Value("${app.credits.direct-chat-cost:10}") private int directCost;
    public record Person(Long id,String nickname,int age,String region,String imageUrl) {}
    public record Connection(Person person,Long roomId,boolean matched,Instant createdAt,String eventKey) {
        public Connection(Person person,Long roomId,boolean matched,Instant createdAt) { this(person,roomId,matched,createdAt,null); }
    }
    public record Overview(List<Connection> received,List<Connection> sent,List<Connection> matched) {}
    public record Room(Long id,Person person,boolean matched,String lastMessage,Instant updatedAt,long unread) {}
    public record Message(Long id,Long senderId,String text,Instant createdAt,String clientId,boolean mine) {}
    public record Messages(Long roomId,Person person,List<Message> messages,boolean hasOlder,long peerReadThrough,boolean matched) {}
    public record Quote(int balance,int cost,Long roomId) {}
    public record SendRequest(String text,String clientId) {}
    public record DirectRequest(Long targetId,String text,String clientId,Integer expectedCost) {}
    public record OpenResult(Long roomId,int charged,int balance) {}

    public record PublicProfile(Person person,List<String> images,String job,String mbti,String introduction,String personality,String idealType,String smoking,String drinking,String religion,int totalImages,int ownImageCount) {}
    @Transactional(readOnly=true) public PublicProfile profile(String email,Long id) {
        var me=user(email); requireAvailable(me.getId(),id); requireKnown(me.getId(),id);
        var p=em.createQuery("select p from UserProfile p where p.userId=:id",UserProfile.class).setParameter("id",id).getResultStream().findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"프로필을 찾을 수 없습니다."));
        var images=em.createQuery("select i.imageUrl from UserProfileImage i where i.userId=:id order by i.isMain desc,i.imageOrder",String.class).setParameter("id",id).getResultList();
        var mineApproved=em.find(ApprovedProfile.class,me.getId());
        int ownCount=em.createQuery("select count(i) from UserProfileImage i where i.userId=:u",Long.class).setParameter("u",me.getId()).getSingleResult().intValue();
        if(mineApproved!=null)ownCount=mineApproved.getImages().size();
        var approved=em.find(ApprovedProfile.class,id);
        if(approved!=null)return new PublicProfile(person(id),approved.getImages().stream().limit(ownCount).toList(),approved.getJob(),approved.getMbti(),approved.getIntroduction(),approved.getPersonality(),approved.getIdealType(),approved.getSmoking(),approved.getDrinking(),approved.getReligion(),approved.getImages().size(),ownCount);
        return new PublicProfile(person(id),images.stream().limit(ownCount).toList(),p.getJob(),p.getMbti(),p.getIntroduction(),p.getPersonality(),p.getIdealType(),p.getSmokingStatus(),p.getDrinkingStatus(),p.getReligion(),images.size(),ownCount);
    }
    @Transactional(readOnly=true) public List<Person> blocks(String email){var me=user(email);return em.createQuery("select v.targetId from MemberVisibility v where v.ownerId=:u and v.blocked=true",Long.class).setParameter("u",me.getId()).getResultList().stream().map(this::person).toList();}
    @Transactional public void unblock(String email,Long id){var me=user(email);lockPair(me.getId(),id);var v=visibility(me.getId(),id,false);if(v!=null&&v.isBlocked()){var target=em.find(User.class,id);long active=em.createQuery("select count(v) from MemberVisibility v where v.targetId=:id and v.blocked=true",Long.class).setParameter("id",id).getSingleResult();target.setTotalBlockCount(Math.max(target.getTotalBlockCount(),active));v.setBlocked(false);}}
    @Transactional public void unmatch(String email,Long id){var me=user(email);lockPair(me.getId(),id);var r=findRoom(me.getId(),id);if(r==null||!r.isMatched())throw error(HttpStatus.NOT_FOUND,"매칭된 상대를 찾을 수 없습니다.");em.lock(r,LockModeType.PESSIMISTIC_WRITE);r.setEnded(true);r.setMatched(false);for(var l:List.of(new Long[]{me.getId(),id},new Long[]{id,me.getId()})){var like=findLike(l[0],l[1]);if(like!=null)like.setActive(false);}}
    @Transactional public void block(String email,Long id) {
        var me=user(email);lockPair(me.getId(),id);requireKnown(me.getId(),id);
        if(em.find(User.class,id)==null)throw error(HttpStatus.NOT_FOUND,"상대를 찾을 수 없습니다.");
        var vis=visibility(me.getId(),id,true);
        if(!vis.isBlocked()){var target=em.find(User.class,id);long active=em.createQuery("select count(v) from MemberVisibility v where v.targetId=:id and v.blocked=true",Long.class).setParameter("id",id).getSingleResult();target.setTotalBlockCount(Math.max(target.getTotalBlockCount(),active)+1);vis.setBlocked(true);}
    }
    @Transactional public void hideMatch(String email,Long id) {
        var me=user(email);lockPair(me.getId(),id);requireKnown(me.getId(),id);
        visibility(me.getId(),id,true).setMatchHidden(true);
    }
    @Transactional public void deleteRoom(String email,Long id) {
        var me=user(email);em.lock(me,LockModeType.PESSIMISTIC_WRITE);var r=authorizedRoom(id,me.getId());
        var v=visibility(me.getId(),other(r,me.getId()),true);v.setChatHidden(true);
        long last=r.getLastMessageId()==null?0:r.getLastMessageId();v.setChatHiddenThrough(last);
        if(r.getLowId().equals(me.getId()))r.setReadLow(last);else r.setReadHigh(last);
    }
    private MemberVisibility visibility(Long me,Long other,boolean create) {
        var v=em.createQuery("select v from MemberVisibility v where v.ownerId=:me and v.targetId=:other",MemberVisibility.class).setParameter("me",me).setParameter("other",other).getResultStream().findFirst().orElse(null);
        if(v==null&&create){v=new MemberVisibility();v.setOwnerId(me);v.setTargetId(other);em.persist(v);}return v;
    }
    private boolean matchHidden(Long me,Long other){var v=visibility(me,other,false);return v!=null&&v.isMatchHidden();}

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Connection like(String email,Long otherId) {
        var me=user(email); lockPair(me.getId(),otherId); requireAvailable(me.getId(),otherId); requireKnown(me.getId(),otherId);
        var like=findLike(me.getId(),otherId);
        if(like==null) { like=new MemberLike(); like.setSenderId(me.getId()); like.setTargetId(otherId); em.persist(like); }
        like.setActive(true);em.flush();inbox.add(otherId,"LIKE:"+like.getId(),"새 좋아요가 도착했어요","매칭현황에서 상대를 확인해 보세요.","/matches");
        var reverse=findLike(otherId,me.getId());
        var room=findRoom(me.getId(),otherId);
        if(reverse!=null&&reverse.isActive()) {
            if(room==null) room=createRoom(me.getId(),otherId);
            else em.refresh(room,LockModeType.PESSIMISTIC_WRITE);
            room.setMatched(true);
            inbox.add(me.getId(),"MATCH:"+room.getId(),"새로운 매칭이 성사됐어요","대화방에서 인사를 건네보세요.","/chats/"+room.getId());
            inbox.add(otherId,"MATCH:"+room.getId(),"새로운 매칭이 성사됐어요","대화방에서 인사를 건네보세요.","/chats/"+room.getId());
        }
        return new Connection(person(otherId),room==null?null:room.getId(),room!=null&&room.isMatched(),like.getCreatedAt());
    }
    @Transactional public void dismiss(String email,Long otherId) {
        var me=user(email); lockPair(me.getId(),otherId);
        var like=findLike(otherId,me.getId());
        if(like==null) throw error(HttpStatus.NOT_FOUND,"좋아요를 찾을 수 없습니다.");
        like.setDismissed(true);
    }
    @Transactional(readOnly=true) public Overview overview(String email) {
        var me=user(email);
        var likes=em.createQuery("select l from MemberLike l where l.active=true and (l.senderId=:id or l.targetId=:id) order by l.createdAt desc",MemberLike.class)
                .setParameter("id",me.getId()).getResultList();
        List<Connection> received=new ArrayList<>(),sent=new ArrayList<>(),matched=new ArrayList<>();
        for(var l:likes) {
            boolean incoming=l.getTargetId().equals(me.getId()); var other=incoming?l.getSenderId():l.getTargetId();
            if(!available(me.getId(),other)||matchHidden(me.getId(),other)) continue;
            var room=findRoom(me.getId(),other);
            var c=new Connection(person(other),room==null?null:room.getId(),room!=null&&room.isMatched(),l.getCreatedAt(),incoming?"LIKE:"+l.getId():null);
            if(incoming&&!l.isDismissed()&&!c.matched()) received.add(c);
            if(!incoming) sent.add(c);
        }
        for(var room:allRooms(me.getId())) if(room.isMatched()&&available(me.getId(),other(room,me.getId()))&&!matchHidden(me.getId(),other(room,me.getId())))
            matched.add(new Connection(person(other(room,me.getId())),room.getId(),true,room.getUpdatedAt(),"MATCH:"+room.getId()));
        return new Overview(received,sent,matched);
    }
    @Transactional(readOnly=true) public Quote quote(String email,Long otherId) {
        var me=user(email); requireAvailable(me.getId(),otherId); requireKnown(me.getId(),otherId);
        var room=findRoom(me.getId(),otherId); var wallet=em.find(CreditWallet.class,me.getId());
        return new Quote(wallet==null?0:wallet.getBalance(),room==null?directCost:0,room==null?null:room.getId());
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public OpenResult direct(String email,DirectRequest request) {
        if(request==null||request.targetId()==null) throw error(HttpStatus.BAD_REQUEST,"상대를 선택해 주세요.");
        validateMessage(new SendRequest(request.text(),request.clientId()));
        var me=user(email); lockPair(me.getId(),request.targetId()); requireAvailable(me.getId(),request.targetId()); requireKnown(me.getId(),request.targetId());
        var room=findRoom(me.getId(),request.targetId());
        var wallet=em.find(CreditWallet.class,me.getId()); int balance=wallet==null?0:wallet.getBalance(); int charged=0;
        if(room==null) {
            if(request.expectedCost()==null||request.expectedCost()!=directCost) throw error(HttpStatus.CONFLICT,"크레딧 비용을 다시 확인해 주세요.");
            if(balance<directCost) throw error(HttpStatus.CONFLICT,"크레딧이 부족합니다. 충전 기능은 준비 중입니다.");
            room=createRoom(me.getId(),request.targetId());
            charged=directCost; balance-=charged; wallet.setBalance(balance);
            var entry=new CreditEntry(); entry.setUserId(me.getId()); entry.setAmount(-charged); entry.setReferenceKey("DIRECT_CHAT:"+room.getId()); em.persist(entry);
        } else em.lock(room,LockModeType.PESSIMISTIC_WRITE);
        append(room,me.getId(),new SendRequest(request.text(),request.clientId()));
        return new OpenResult(room.getId(),charged,balance);
    }
    @Transactional(readOnly=true) public List<Room> rooms(String email) {
        var me=user(email); List<Room> result=new ArrayList<>();
        for(var r:allRooms(me.getId())) {
            var other=other(r,me.getId()); if(!available(me.getId(),other)) continue;
            var visibility=visibility(me.getId(),other,false);
            if(visibility!=null&&visibility.isChatHidden()&&(r.getLastMessageId()==null?0:r.getLastMessageId())<=visibility.getChatHiddenThrough())continue;
            long read=r.getLowId().equals(me.getId())?r.getReadLow():r.getReadHigh();
            long unread=em.createQuery("select count(m) from ChatMessage m where m.roomId=:room and m.id>:read and m.senderId<>:me",Long.class)
                    .setParameter("room",r.getId()).setParameter("read",read).setParameter("me",me.getId()).getSingleResult();
            result.add(new Room(r.getId(),person(other),r.isMatched(),r.getLastMessage(),r.getUpdatedAt(),unread));
        }
        return result;
    }
    @Transactional public Messages messages(String email,Long roomId,long after,long before) {
        var me=user(email); var room=authorizedRoom(roomId,me.getId());
        if(after<0||before<0||after>0&&before>0) throw error(HttpStatus.BAD_REQUEST,"잘못된 메시지 위치입니다.");
        var visibility=visibility(me.getId(),other(room,me.getId()),false);
        long hiddenThrough=visibility==null?0:visibility.getChatHiddenThrough();
        String filter=after>0?" and m.id>:cursor":before>0?" and m.id<:cursor":"";
        var q=em.createQuery("select m from ChatMessage m where m.roomId=:room and m.id>:hidden"+filter+" order by m.id "+(after>0?"asc":"desc"),ChatMessage.class).setParameter("room",roomId).setParameter("hidden",hiddenThrough);
        if(after>0||before>0) q.setParameter("cursor",after>0?after:before);
        var messages=new ArrayList<>(q.setMaxResults(100).getResultList()); if(after==0) Collections.reverse(messages);
        if(!messages.isEmpty()) {
            long last=messages.get(messages.size()-1).getId();
            if(room.getLowId().equals(me.getId())) room.setReadLow(Math.max(room.getReadLow(),last)); else room.setReadHigh(Math.max(room.getReadHigh(),last));
        }
        boolean older=!messages.isEmpty()&&em.createQuery("select count(m) from ChatMessage m where m.roomId=:room and m.id>:hidden and m.id<:first",Long.class)
                .setParameter("room",roomId).setParameter("hidden",hiddenThrough).setParameter("first",messages.get(0).getId()).getSingleResult()>0;
        return new Messages(roomId,person(other(room,me.getId())),messages.stream().map(m->dto(m,me.getId())).toList(),older,room.getLowId().equals(me.getId())?room.getReadHigh():room.getReadLow(),room.isMatched());
    }
    @Transactional public Message send(String email,Long roomId,SendRequest request) {
        validateMessage(request); var me=user(email); return dto(append(authorizedRoom(roomId,me.getId()),me.getId(),request),me.getId());
    }
    private ChatMessage append(ChatRoom room,Long sender,SendRequest request) {
        var existing=em.createQuery("select m from ChatMessage m where m.roomId=:room and m.senderId=:sender and m.clientId=:client",ChatMessage.class)
                .setParameter("room",room.getId()).setParameter("sender",sender).setParameter("client",request.clientId()).getResultStream().findFirst().orElse(null);
        if(existing!=null) return existing;
        var m=new ChatMessage(); m.setRoomId(room.getId()); m.setSenderId(sender); m.setClientId(request.clientId()); m.setBody(request.text().trim()); em.persist(m); em.flush();
        inbox.add(other(room,sender),"CHAT:"+m.getId(),"새 메시지가 도착했어요","채팅방에서 메시지를 확인해 주세요.","/chats/"+room.getId());
        room.setLastMessageId(m.getId()); room.setLastMessage(m.getBody()); room.setUpdatedAt(m.getCreatedAt());
        if(room.getLowId().equals(sender)) room.setReadLow(m.getId()); else room.setReadHigh(m.getId());
        return m;
    }
    private void validateMessage(SendRequest r) {
        if(r==null||r.text()==null||r.text().isBlank()||r.text().length()>2000) throw error(HttpStatus.BAD_REQUEST,"메시지는 1~2,000자로 입력해 주세요.");
        try { if(r.clientId()==null||r.clientId().length()!=36) throw new IllegalArgumentException(); UUID.fromString(r.clientId()); } catch(Exception e) { throw error(HttpStatus.BAD_REQUEST,"메시지 요청 번호가 올바르지 않습니다."); }
    }
    private ChatRoom authorizedRoom(Long id,Long me) {
        var room=em.find(ChatRoom.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(room==null||!room.getLowId().equals(me)&&!room.getHighId().equals(me)) throw error(HttpStatus.NOT_FOUND,"대화방을 찾을 수 없습니다.");
        requireAvailable(me,other(room,me)); return room;
    }
    private List<ChatRoom> allRooms(Long me) { return em.createQuery("select r from ChatRoom r where r.lowId=:id or r.highId=:id order by r.updatedAt desc",ChatRoom.class).setParameter("id",me).getResultList(); }
    private ChatRoom createRoom(Long a,Long b) { var r=new ChatRoom(); r.setLowId(Math.min(a,b)); r.setHighId(Math.max(a,b)); em.persist(r); em.flush(); return r; }
    private ChatRoom findRoom(Long a,Long b) { return em.createQuery("select r from ChatRoom r where r.lowId=:a and r.highId=:b",ChatRoom.class).setParameter("a",Math.min(a,b)).setParameter("b",Math.max(a,b)).getResultStream().findFirst().orElse(null); }
    private MemberLike findLike(Long a,Long b) { return em.createQuery("select l from MemberLike l where l.senderId=:a and l.targetId=:b",MemberLike.class).setParameter("a",a).setParameter("b",b).getResultStream().findFirst().orElse(null); }
    private Long other(ChatRoom r,Long me) { return r.getLowId().equals(me)?r.getHighId():r.getLowId(); }
    private void lockPair(Long a,Long b) {
        if(b==null||a.equals(b)) throw error(HttpStatus.BAD_REQUEST,"다른 회원을 선택해 주세요.");
        em.createQuery("select u from User u where u.id in :ids order by u.id",User.class).setParameter("ids",List.of(a,b)).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }
    private User user(String email) {
        var u=em.createQuery("select u from User u where u.email=:email",User.class).setParameter("email",email).getResultStream().findFirst().orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"로그인이 필요합니다."));
        if(!"ACTIVE".equals(u.getStatus())) throw error(HttpStatus.FORBIDDEN,"승인된 계정만 이용할 수 있습니다."); return u;
    }
    private void requireKnown(Long me,Long other) {
        if(findRoom(me,other)!=null||findLike(other,me)!=null||findLike(me,other)!=null) return;
        long count=em.createQuery("select count(c) from DailyMatchCard c where c.ownerId=:me and c.candidateId=:other and c.cycleDay=:day and c.revealedAt is not null",Long.class)
                .setParameter("me",me).setParameter("other",other).setParameter("day",schedule.current().day()).getSingleResult();
        if(count==0) throw error(HttpStatus.FORBIDDEN,"공개된 인연에게만 보낼 수 있습니다.");
    }
    private void requireAvailable(Long a,Long b) { if(!available(a,b)) throw error(HttpStatus.FORBIDDEN,"현재 대화할 수 없는 상대입니다."); }
    boolean available(Long a,Long b) {
        if(b==null||a.equals(b)) return false;
        var me=em.find(User.class,a); var other=em.find(User.class,b);
        if(me==null||other==null||!"ACTIVE".equals(me.getStatus())||!"ACTIVE".equals(other.getStatus())) return false;
        long reports=em.createQuery("select count(r) from UserReport r where (r.reporterUserId=:a and r.reportedUserId=:b) or (r.reporterUserId=:b and r.reportedUserId=:a)",Long.class).setParameter("a",a).setParameter("b",b).getSingleResult();
        long blocks=em.createQuery("select count(v) from MemberVisibility v where v.blocked=true and ((v.ownerId=:a and v.targetId=:b) or (v.ownerId=:b and v.targetId=:a))",Long.class).setParameter("a",a).setParameter("b",b).getSingleResult();
        var room=findRoom(a,b);return reports==0&&blocks==0&&(room==null||!room.isEnded());
    }
    private Person person(Long id) {
        var u=em.find(User.class,id);
        var approved=em.find(ApprovedProfile.class,id);
        if(approved!=null)return new Person(id,approved.getNickname(),approved.getBirthDate()==null?0:Period.between(approved.getBirthDate(),schedule.current().day()).getYears(),approved.getRegion(),approved.getImages().isEmpty()?null:approved.getImages().get(0));
        var p=em.createQuery("select p from UserProfile p where p.userId=:id",UserProfile.class).setParameter("id",id).getResultStream().findFirst().orElse(null);
        var v=em.createQuery("select v from UserVerification v where v.userId=:id",UserVerification.class).setParameter("id",id).getResultStream().findFirst().orElse(null);
        var image=em.createQuery("select i.imageUrl from UserProfileImage i where i.userId=:id order by i.isMain desc,i.imageOrder",String.class).setParameter("id",id).setMaxResults(1).getResultStream().findFirst().orElse(null);
        return new Person(id,u.getNickname(),v==null?0:Period.between(v.getBirthDate(),schedule.current().day()).getYears(),p==null?"":p.getRegion(),image);
    }
    private Message dto(ChatMessage m,Long me) { return new Message(m.getId(),m.getSenderId(),m.getBody(),m.getCreatedAt(),m.getClientId(),m.getSenderId().equals(me)); }
    private ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
