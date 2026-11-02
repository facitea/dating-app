package com.dating.backend.matching;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.List;

@Service @RequiredArgsConstructor
public class ChatRequestService {
    private final EntityManager em;
    private final InboxService inbox;
    public record Status(String state,boolean incoming,Instant expiresAt,int credits) {}
    public Status status(Long room,Long viewer) {
        var r=em.find(DirectChatRequest.class,room);
        return r==null?new Status("ACCEPTED",false,null,0):new Status(r.getState(),r.getRecipientId().equals(viewer),r.getExpiresAt(),r.getCost());
    }
    // Caller holds the chat-room write lock. All transitions and refunds use that same lock.
    public void create(ChatRoom room,Long sender,Long recipient,int cost) {
        var r=new DirectChatRequest();r.setRoomId(room.getId());r.setSenderId(sender);r.setRecipientId(recipient);
        r.setCost(cost);r.setExpiresAt(Instant.now().plus(Duration.ofHours(72)));em.persist(r);
    }
    public void requireSendAllowed(ChatRoom room,Long sender) {
        var r=em.find(DirectChatRequest.class,room.getId());
        if(r==null||"ACCEPTED".equals(r.getState()))return;
        if("PENDING".equals(r.getState())&&Instant.now().isBefore(r.getExpiresAt())&&room.getLastMessageId()==null&&sender.equals(r.getSenderId()))return;
        throw new ResponseStatusException(HttpStatus.CONFLICT,"상대가 대화 요청을 수락한 후 메시지를 보낼 수 있습니다.");
    }
    public void expire(ChatRoom room) {
        var r=em.find(DirectChatRequest.class,room.getId());
        if(r!=null&&"PENDING".equals(r.getState())&&!Instant.now().isBefore(r.getExpiresAt()))finish(r,"EXPIRED",true);
    }
    public Status decide(ChatRoom room,Long viewer,boolean accept) {
        var r=em.find(DirectChatRequest.class,room.getId());
        if(r==null||!r.getRecipientId().equals(viewer))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"요청을 받은 회원만 수락하거나 거절할 수 있습니다.");
        expire(room);
        if("PENDING".equals(r.getState())) {
            finish(r,accept?"ACCEPTED":"DECLINED",!accept);
            if(accept)inbox.add(r.getSenderId(),"CHAT_ACCEPT:"+r.getRoomId(),"대화 요청이 수락됐어요","이제 자유롭게 대화해 보세요.","/chats/"+r.getRoomId());
        }
        return status(room.getId(),viewer);
    }
    public void matched(ChatRoom room) {
        var r=em.find(DirectChatRequest.class,room.getId());
        if(r==null)return;
        if("PENDING".equals(r.getState()))finish(r,"ACCEPTED",true);
        else if(!"ACCEPTED".equals(r.getState()))r.setState("ACCEPTED");
    }
    private void finish(DirectChatRequest r,String state,boolean refund) {
        r.setState(state);
        if(!refund)return;
        var wallet=em.find(CreditWallet.class,r.getSenderId(),LockModeType.PESSIMISTIC_WRITE);
        if(wallet==null)throw new IllegalStateException("Paid chat wallet missing");
        wallet.setBalance(Math.addExact(wallet.getBalance(),r.getCost()));
        var e=new CreditEntry();e.setUserId(r.getSenderId());e.setAmount(r.getCost());e.setReferenceKey("DIRECT_REFUND:"+r.getRoomId());em.persist(e);
        inbox.add(r.getSenderId(),"CREDIT_REFUND:"+r.getRoomId(),"대화 요청 크레딧이 반환됐어요",
            ("ACCEPTED".equals(state)?"서로 좋아요를 보내 무료 대화로 전환됐어요.":"DECLINED".equals(state)?"대화 요청이 거절됐어요.":"72시간 안에 수락되지 않았어요.")+" "+r.getCost()+"크레딧을 돌려드렸어요.","/credits");
    }
    @Transactional(readOnly=true) public List<Long> expiredIds() {
        return em.createQuery("select r.roomId from DirectChatRequest r where r.state='PENDING' and r.expiresAt<=:now order by r.expiresAt",Long.class)
            .setParameter("now",Instant.now()).setMaxResults(100).getResultList();
    }
    @Transactional public void expireOne(Long id) {
        var room=em.find(ChatRoom.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(room!=null)expire(room);
    }
}
