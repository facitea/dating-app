package com.dating.backend.matching;

import com.dating.backend.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyMatchService {
    private final EntityManager em;
    private final DailyMatchSchedule schedule;
    private final MatchPreferenceService preferences;
    private final SocialService social;

    public record Profile(Long userId, String nickname, int age, String region, String job,
                          String mbti, String introduction, String imageUrl) {}
    public record Card(int slot, Instant unlockAt, String state, String reaction, Profile profile) {}
    public record Deck(LocalDate cycleDay, Instant serverNow, Instant resetAt, Instant nextRefreshAt,
                       List<Card> cards,String eventKey) {}
    private record Candidate(User user, UserProfile profile, UserVerification verification, String imageUrl) {}

    @Transactional
    public Deck today(String email) {
        User owner = owner(email);
        var window = schedule.current();
        if(owner.isResting())return new Deck(window.day(),window.now(),window.resetAt(),window.now().isBefore(window.evening())?window.evening():window.resetAt(),java.util.stream.IntStream.range(0,6).mapToObj(i->new Card(i,window.unlockAt(i),"PAUSED",null,null)).toList(),NotificationService.todayKey(window));
        var cards = cards(owner.getId(), window.day());
        if (cards.isEmpty()) {
            for(int slot=0;slot<6;slot++) { var c=new DailyMatchCard(); c.setOwnerId(owner.getId()); c.setCycleDay(window.day()); c.setSlot(slot); em.persist(c); }
            cards=cards(owner.getId(),window.day());
        }
        generateRound(owner,window,cards,0);
        if(!window.now().isBefore(window.evening())) generateRound(owner,window,cards,3);
        return new Deck(window.day(), window.now(), window.resetAt(),
                window.now().isBefore(window.evening()) ? window.evening() : window.resetAt(),
                cards.stream().map(card -> view(card, window)).toList(),NotificationService.todayKey(window));
    }

    @Transactional
    public Card reveal(String email, LocalDate day, int slot) {
        User owner = owner(email);
        var window = schedule.current();
        var card = accessible(owner, day, slot, window);
        if (card.getCandidateId() == null) throw error(HttpStatus.CONFLICT, "아직 추천할 인연을 준비 중이에요.");
        if (candidate(owner.getId(), card.getCandidateId(), window.day()) == null)
            throw error(HttpStatus.CONFLICT, "지금은 이 인연을 확인할 수 없어요.");
        if (card.getRevealedAt() == null) card.setRevealedAt(window.now());
        return view(card, window);
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Card react(String email, LocalDate day, int slot, String reaction) {
        if (!Set.of("LIKE", "PASS").contains(reaction == null ? "" : reaction))
            throw error(HttpStatus.BAD_REQUEST, "올바른 반응을 선택해 주세요.");
        User owner = em.createQuery("select u from User u where u.email=:email",User.class).setParameter("email",email).getResultStream().findFirst().orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"로그인이 필요합니다."));
        var window = schedule.current();
        var card = accessible(owner, day, slot, window);
        if (card.getRevealedAt() == null || card.getCandidateId() == null)
            throw error(HttpStatus.CONFLICT, "카드를 먼저 열어 주세요.");
        if (candidate(owner.getId(), card.getCandidateId(), window.day()) == null)
            throw error(HttpStatus.CONFLICT, "지금은 이 인연을 확인할 수 없어요.");
        if("LIKE".equals(reaction)) social.like(email,card.getCandidateId());
        else if(!"ACTIVE".equals(owner.getStatus())) throw error(HttpStatus.FORBIDDEN,"승인 후 이용해 주세요.");
        card.setReaction(reaction);
        return view(card, window);
    }

    public record ExtraQuote(int cost,int balance,int purchasedCount,LocalDate cycleDay,Instant resetAt){}
    public record ExtraPurchase(LocalDate cycleDay,int expectedCost,String requestId){}
    @Transactional public ExtraQuote extraQuote(String email){var u=owner(email);var w=schedule.current();var wallet=em.find(CreditWallet.class,u.getId());return new ExtraQuote(10,wallet==null?0:wallet.getBalance(),(int)cards(u.getId(),w.day()).stream().filter(c->c.getSlot()>=6).count(),w.day(),w.resetAt());}
    @Transactional public Card buyExtra(String email,ExtraPurchase request){
        var u=owner(email);var window=schedule.current();
        if(request==null||!window.day().equals(request.cycleDay()))throw error(HttpStatus.CONFLICT,"새 회차로 바뀌었습니다. 가격을 다시 확인해 주세요.");
        if(u.isResting())throw error(HttpStatus.CONFLICT,"휴식 모드를 해제해 주세요.");
        if(request.expectedCost()!=10)throw error(HttpStatus.CONFLICT,"가격을 다시 확인해 주세요.");
        try{if(!UUID.fromString(request.requestId()).toString().equals(request.requestId()))throw new IllegalArgumentException();}catch(Exception e){throw error(HttpStatus.BAD_REQUEST,"구매 요청 번호를 확인해 주세요.");}
        var existing=cards(u.getId(),window.day()).stream().filter(c->request.requestId().equals(c.getPurchaseRequestId())).findFirst();if(existing.isPresent())return view(existing.get(),window);
        today(email);
        var wallet=em.find(CreditWallet.class,u.getId(),LockModeType.PESSIMISTIC_WRITE);if(wallet==null||wallet.getBalance()<10)throw error(HttpStatus.CONFLICT,"크레딧이 부족합니다. 충전 후 다시 시도해 주세요.");
        var excluded=cards(u.getId(),window.day()).stream().map(DailyMatchCard::getCandidateId).filter(Objects::nonNull).collect(Collectors.toSet());
        cards(u.getId(),window.day().minusDays(1)).stream().map(DailyMatchCard::getCandidateId).filter(Objects::nonNull).forEach(excluded::add);
        var pref=preferences.active(u.getId(),window.now().isBefore(window.evening())?window.morning():window.evening());var mine=profile(u.getId());var verification=verification(u.getId());
        if(verification==null||verification.getBirthDate()==null)throw error(HttpStatus.CONFLICT,"프로필을 먼저 완성해 주세요.");
        var pool=new ArrayList<Candidate>();for(var other:em.createQuery("select u from User u where u.status='ACTIVE' and u.id<>:id",User.class).setParameter("id",u.getId()).getResultList()){if(excluded.contains(other.getId()))continue;var c=candidate(u.getId(),other.getId(),window.day());if(c!=null&&preferences.accepts(pref,mine,c.profile(),c.verification(),window.day()))pool.add(c);}
        pool.sort(Comparator.<Candidate>comparingInt(c->score(mine,verification,c)).reversed().thenComparingInt(c->Objects.hash(u.getId(),window.day(),c.user().getId())));
        if(pool.isEmpty())throw error(HttpStatus.CONFLICT,"조건에 맞는 추가 인연이 없습니다. 크레딧은 차감되지 않았습니다.");
        var card=new DailyMatchCard();card.setOwnerId(u.getId());card.setCycleDay(window.day());card.setSlot(Math.max(5,cards(u.getId(),window.day()).stream().mapToInt(DailyMatchCard::getSlot).max().orElse(5))+1);card.setPurchaseRequestId(request.requestId());card.setCandidateId(pool.get(0).user().getId());card.setRevealedAt(window.now());em.persist(card);
        wallet.setBalance(wallet.getBalance()-10);var entry=new CreditEntry();entry.setUserId(u.getId());entry.setAmount(-10);entry.setReferenceKey("EXTRA:"+window.day()+":"+request.requestId());em.persist(entry);em.flush();return view(card,window);
    }

    private User owner(String email) {
        // Serialize requests for this user, including first visit and simultaneous taps.
        var owners = em.createQuery("select u from User u where u.email = :email", User.class)
                .setParameter("email", email).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        if (owners.isEmpty()) throw error(HttpStatus.UNAUTHORIZED, "로그인 후 이용해 주세요.");
        var owner = owners.get(0);
        if (!"ACTIVE".equals(owner.getStatus())) throw error(HttpStatus.FORBIDDEN, "승인된 계정만 인연을 확인할 수 있어요.");
        preferences.requireConfigured(owner.getId());
        return owner;
    }

    private List<DailyMatchCard> cards(Long owner, LocalDate day) {
        return em.createQuery("select c from DailyMatchCard c where c.ownerId = :owner and c.cycleDay = :day order by c.slot", DailyMatchCard.class)
                .setParameter("owner", owner).setParameter("day", day).getResultList();
    }

    private DailyMatchCard accessible(User owner, LocalDate day, int slot, DailyMatchSchedule.Window window) {
        preferences.requireConfigured(owner.getId());
        if(owner.isResting())throw error(HttpStatus.CONFLICT,"휴식 모드를 해제한 뒤 이용해 주세요.");
        if (!window.day().equals(day)) throw error(HttpStatus.CONFLICT, "새로운 인연으로 갱신됐어요. 다시 확인해 주세요.");
        if (slot < 0) throw error(HttpStatus.NOT_FOUND, "카드를 찾을 수 없어요.");
        if (!window.unlocked(slot)) throw error(HttpStatus.FORBIDDEN, "아직 카드가 열리는 시간이 아니에요.");
        return cards(owner.getId(), day).stream().filter(c -> c.getSlot() == slot).findFirst()
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "오늘의 인연을 먼저 불러와 주세요."));
    }

    private void generateRound(User owner, DailyMatchSchedule.Window window, List<DailyMatchCard> cards, int first) {
        Instant start=first==0?window.morning():window.evening();
        if(em.createQuery("select count(r) from MatchRound r where r.ownerId=:owner and r.startsAt=:start",Long.class)
                .setParameter("owner",owner.getId()).setParameter("start",start).getSingleResult()>0) return;
        var pref=preferences.active(owner.getId(),start);
        var marker=new MatchRound(); marker.setOwnerId(owner.getId()); marker.setStartsAt(start);
        marker.setPreferenceId(pref==null?null:pref.getId()); em.persist(marker);
        // Preserve existing opened cards during migration; discard only unrevealed future reservations.
        if(pref==null && first==0 && cards.stream().limit(3).anyMatch(c->c.getCandidateId()!=null)) return;
        var excluded=cards(owner.getId(),window.day().minusDays(1)).stream().map(DailyMatchCard::getCandidateId).filter(Objects::nonNull).collect(Collectors.toSet());
        for(var c:cards) if(c.getSlot()<first || c.getRevealedAt()!=null || c.getSlot()>=6) if(c.getCandidateId()!=null) excluded.add(c.getCandidateId());
        var mine=profile(owner.getId()); var verification=verification(owner.getId());
        List<Candidate> pool=new ArrayList<>();
        if(verification!=null && verification.getBirthDate()!=null && !verification.getBirthDate().isAfter(window.day().minusYears(18))) {
            for(var u:em.createQuery("select u from User u where u.status='ACTIVE' and u.id<>:id",User.class).setParameter("id",owner.getId()).getResultList()) {
                if(excluded.contains(u.getId())) continue;
                var c=candidate(owner.getId(),u.getId(),window.day());
                if(c!=null && preferences.accepts(pref,mine,c.profile(),c.verification(),window.day())) pool.add(c);
            }
            pool.sort(Comparator.<Candidate>comparingInt(c->score(mine,verification,c)).reversed().thenComparingInt(c->Objects.hash(owner.getId(),start,c.user().getId())));
        }
        int index=0;
        for(var card:cards) if(card.getSlot()>=first&&card.getSlot()<first+3&&card.getRevealedAt()==null) {
            card.setCandidateId(index<pool.size()?pool.get(index++).user().getId():null);
        }
    }

    private int score(UserProfile mine, UserVerification verification, Candidate c) {
        int score = 0;
        if (mine != null && mine.getRegion() != null && c.profile().getRegion() != null) {
            if (mine.getRegion().equals(c.profile().getRegion())) score += 100;
            else if (mine.getRegion().split(" ")[0].equals(c.profile().getRegion().split(" ")[0])) score += 50;
        }
        return score - Math.abs(Period.between(verification.getBirthDate(), c.verification().getBirthDate()).getYears());
    }

    private Candidate candidate(Long owner, Long id, LocalDate day) {
        var user = em.find(User.class, id);
        if (user == null || user.isResting() || "PENDING".equals(user.getProfileReviewStatus()) || "REJECTED".equals(user.getProfileReviewStatus()) || !"ACTIVE".equals(user.getStatus()) || owner.equals(id)
                || !"NORMAL".equals(user.getFraudReviewStatus())) return null;
        var mine = verification(owner);
        var other = verification(id);
        if (mine == null || other == null || other.getBirthDate() == null
                || other.getBirthDate().isAfter(day.minusYears(18))) return null;
        String expected = "MALE".equals(mine.getGender()) ? "FEMALE" : "FEMALE".equals(mine.getGender()) ? "MALE" : "";
        if (expected.isEmpty() || !expected.equals(other.getGender())) return null;
        long reports = em.createQuery("select count(r) from UserReport r where (r.reporterUserId = :owner and r.reportedUserId = :other) or (r.reporterUserId = :other and r.reportedUserId = :owner)", Long.class)
                .setParameter("owner", owner).setParameter("other", id).getSingleResult();
        if (reports > 0) return null;
        if(em.createQuery("select count(r) from ChatRoom r where r.ended=true and r.lowId=:a and r.highId=:b",Long.class).setParameter("a",Math.min(owner,id)).setParameter("b",Math.max(owner,id)).getSingleResult()>0)return null;
        long blocks=em.createQuery("select count(v) from MemberVisibility v where v.blocked=true and ((v.ownerId=:owner and v.targetId=:other) or (v.ownerId=:other and v.targetId=:owner))",Long.class).setParameter("owner",owner).setParameter("other",id).getSingleResult();
        if(blocks>0)return null;
        var profile = profile(id);
        if (profile == null || profile.getRegion() == null || profile.getRegion().isBlank()) return null;
        var photos = em.createQuery("select i from UserProfileImage i where i.userId = :id order by i.isMain desc, i.imageOrder asc", UserProfileImage.class)
                .setParameter("id", id).setMaxResults(1).getResultList();
        if (photos.isEmpty()) return null;
        return new Candidate(user, profile, other, photos.get(0).getImageUrl());
    }

    private Card view(DailyMatchCard card, DailyMatchSchedule.Window window) {
        // Locked and unrevealed cards never serialize a candidate ID, photo or personal details.
        if (!window.unlocked(card.getSlot())) return new Card(card.getSlot(), window.unlockAt(card.getSlot()), "LOCKED", null, null);
        if (card.getCandidateId() == null) return new Card(card.getSlot(), window.unlockAt(card.getSlot()), "EMPTY", null, null);
        var c = candidate(card.getOwnerId(), card.getCandidateId(), window.day());
        if (c == null) return new Card(card.getSlot(), window.unlockAt(card.getSlot()), "UNAVAILABLE", null, null);
        if (card.getRevealedAt() == null) return new Card(card.getSlot(), window.unlockAt(card.getSlot()), "HIDDEN", null, null);
        var publicProfile = new Profile(c.user().getId(), c.user().getNickname(), Period.between(c.verification().getBirthDate(), window.day()).getYears(),
                c.profile().getRegion(), c.profile().getJob(), c.profile().getMbti(), c.profile().getIntroduction(), c.imageUrl());
        return new Card(card.getSlot(), window.unlockAt(card.getSlot()), "OPEN", card.getReaction(), publicProfile);
    }

    private UserProfile profile(Long id) {
        return em.createQuery("select p from UserProfile p where p.userId = :id", UserProfile.class)
                .setParameter("id", id).getResultStream().findFirst().orElse(null);
    }
    private UserVerification verification(Long id) {
        return em.createQuery("select v from UserVerification v where v.userId = :id", UserVerification.class)
                .setParameter("id", id).getResultStream().findFirst().orElse(null);
    }
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
}
