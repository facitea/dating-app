package com.dating.backend.matching;
import com.dating.backend.entity.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class MatchPreferenceService {
    private final EntityManager em;
    private final DailyMatchSchedule schedule;
    private final RegionDistance regions;
    public record Conditions(Integer ageMin,Integer ageMax,String regionScope,String mbti,String smoking,String drinking,String religion,
                             Integer birthYearMin,Integer birthYearMax,Integer radiusKm) {
        public Conditions(Integer ageMin,Integer ageMax,String regionScope,String mbti,String smoking,String drinking,String religion) {
            this(ageMin,ageMax,regionScope,mbti,smoking,drinking,religion,null,null,null);
        }
    }
    public record Settings(Conditions active, Conditions pending, Instant effectiveAt, Instant nextRoundAt, String region, boolean radiusAvailable, int referenceYear, boolean configured) {}
    static final Set<String> MBTI = Set.of("ANY","INTJ","INTP","ENTJ","ENTP","INFJ","INFP","ENFJ","ENFP","ISTJ","ISFJ","ESTJ","ESFJ","ISTP","ISFP","ESTP","ESFP");
    @Transactional(readOnly=true) public Settings get(String email) { return settings(owner(email, false)); }
    @Transactional public Settings save(String email, Conditions c) {
        var owner=owner(email,true); validate(c);
        if(c.radiusKm()!=null && !regions.known(region(owner.getId())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"프로필의 거주 지역을 먼저 선택해 주세요.");
        var w=schedule.current(); boolean first=!configured(owner.getId());
        var next=first?w.morning():(w.now().isBefore(w.evening())?w.evening():w.resetAt());
        if(first) {
            // Re-evaluate only unopened free cards created before onboarding was introduced.
            em.createQuery("delete from MatchRound r where r.ownerId=:id and r.startsAt>=:start and r.startsAt<:end")
                .setParameter("id",owner.getId()).setParameter("start",w.morning()).setParameter("end",w.resetAt()).executeUpdate();
            for(var card:em.createQuery("select c from DailyMatchCard c where c.ownerId=:id and c.cycleDay=:day and c.slot<6 and c.revealedAt is null",DailyMatchCard.class)
                .setParameter("id",owner.getId()).setParameter("day",w.day()).getResultList()) card.setCandidateId(null);
        }
        var pending=em.createQuery("select p from MatchPreference p where p.ownerId=:id and p.effectiveAt=:at",MatchPreference.class)
                .setParameter("id",owner.getId()).setParameter("at",next).getResultStream().findFirst().orElseGet(MatchPreference::new);
        pending.setOwnerId(owner.getId()); pending.setEffectiveAt(next);
        pending.setBirthYearMin(c.birthYearMin()); pending.setBirthYearMax(c.birthYearMax()); pending.setRadiusKm(c.radiusKm());
        pending.setAgeMin(c.ageMin()); pending.setAgeMax(c.ageMax()); pending.setRegionScope(c.regionScope());
        pending.setMbti(c.mbti()); pending.setSmoking(c.smoking()); pending.setDrinking(c.drinking()); pending.setReligion(c.religion());
        if(pending.getId()==null) em.persist(pending);
        return settings(owner);
    }
    public boolean configured(Long ownerId) {
        return em.createQuery("select count(p) from MatchPreference p where p.ownerId=:id",Long.class)
            .setParameter("id",ownerId).getSingleResult()>0;
    }
    public void requireConfigured(Long ownerId) {
        if(!configured(ownerId)) throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,"추천조건을 먼저 설정해 주세요.");
    }
    public MatchPreference active(Long ownerId, Instant roundStart) {
        return em.createQuery("select p from MatchPreference p where p.ownerId=:id and p.effectiveAt<=:at order by p.effectiveAt desc,p.id desc",MatchPreference.class)
                .setParameter("id",ownerId).setParameter("at",roundStart).setMaxResults(1).getResultStream().findFirst().orElse(null);
    }
    public boolean accepts(MatchPreference p, UserProfile mine, UserProfile other, UserVerification verification, LocalDate day) {
        if(p==null) return true;
        int year=verification.getBirthDate().getYear();
        if(p.getBirthYearMin()!=null && year<p.getBirthYearMin() || p.getBirthYearMax()!=null && year>p.getBirthYearMax()) return false;
        if(p.getRadiusKm()!=null && !regions.within(mine==null?null:mine.getRegion(),other==null?null:other.getRegion(),p.getRadiusKm())) return false;
        int age=Period.between(verification.getBirthDate(),day).getYears();
        if(p.getAgeMin()!=null&&age<p.getAgeMin() || p.getAgeMax()!=null&&age>p.getAgeMax()) return false;
        if(p.getRadiusKm()==null && !"ANY".equals(p.getRegionScope())) {
            if(mine==null||mine.getRegion()==null||other.getRegion()==null) return false;
            String a=mine.getRegion().trim(),b=other.getRegion().trim();
            if("CITY".equals(p.getRegionScope())&&!a.split("\\s+")[0].equals(b.split("\\s+")[0])) return false;
            if("DISTRICT".equals(p.getRegionScope())&&!a.equals(b)) return false;
        }
        return equal(p.getMbti(),other.getMbti())&&equal(p.getSmoking(),other.getSmokingStatus())
                &&equal(p.getDrinking(),other.getDrinkingStatus())&&equal(p.getReligion(),other.getReligion());
    }
    private boolean equal(String desired,String actual) { return "ANY".equals(desired)||Objects.equals(desired,actual); }
    private Settings settings(User owner) {
        var w=schedule.current(); var start=w.now().isBefore(w.evening())?w.morning():w.evening();
        var pending=em.createQuery("select p from MatchPreference p where p.ownerId=:id and p.effectiveAt>:now order by p.effectiveAt desc",MatchPreference.class)
                .setParameter("id",owner.getId()).setParameter("now",w.now()).setMaxResults(1).getResultStream().findFirst().orElse(null);
        return new Settings(dto(active(owner.getId(),start)),pending==null?null:dto(pending),pending==null?null:pending.getEffectiveAt(),w.now().isBefore(w.evening())?w.evening():w.resetAt(),region(owner.getId()),regions.known(region(owner.getId())),w.now().atZone(DailyMatchSchedule.ZONE).getYear(),configured(owner.getId()));
    }
    private Conditions dto(MatchPreference p) { return p==null?new Conditions(null,null,"ANY","ANY","ANY","ANY","ANY"):new Conditions(p.getAgeMin(),p.getAgeMax(),p.getRegionScope(),p.getMbti(),p.getSmoking(),p.getDrinking(),p.getReligion(),p.getBirthYearMin(),p.getBirthYearMax(),p.getRadiusKm()); }
    private String region(Long id) {
        return em.createQuery("select p.region from UserProfile p where p.userId=:id",String.class).setParameter("id",id).getResultStream().findFirst().orElse("");
    }
    private User owner(String email,boolean lock) {
        var q=em.createQuery("select u from User u where u.email=:email",User.class).setParameter("email",email);
        if(lock) q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        var u=q.getResultStream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if(!"ACTIVE".equals(u.getStatus())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"승인 후 이용할 수 있습니다.");
        return u;
    }
    private void validate(Conditions c) {
        int year=schedule.current().now().atZone(DailyMatchSchedule.ZONE).getYear();
        if(c==null||c.birthYearMin()!=null&&(c.birthYearMin()<1900||c.birthYearMin()>year-18)
          ||c.birthYearMax()!=null&&(c.birthYearMax()<1900||c.birthYearMax()>year-18)
          ||c.birthYearMin()!=null&&c.birthYearMax()!=null&&c.birthYearMin()>c.birthYearMax()
          ||c.radiusKm()!=null&&(c.radiusKm()<5||c.radiusKm()>500||c.radiusKm()%5!=0)
          ||c.ageMin()!=null&&(c.ageMin()<18||c.ageMin()>100)||c.ageMax()!=null&&(c.ageMax()<18||c.ageMax()>100)
          ||c.ageMin()!=null&&c.ageMax()!=null&&c.ageMin()>c.ageMax()
          ||!Set.of("ANY","CITY","DISTRICT").contains(Objects.toString(c.regionScope(),""))||!MBTI.contains(Objects.toString(c.mbti(),""))
          ||!Set.of("ANY","NON_SMOKER","SMOKER","OCCASIONAL").contains(Objects.toString(c.smoking(),""))
          ||!Set.of("ANY","NONE","SOMETIMES","OFTEN").contains(Objects.toString(c.drinking(),""))
          ||!Set.of("ANY","NONE","CHRISTIAN","CATHOLIC","BUDDHIST","OTHER").contains(Objects.toString(c.religion(),"")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"추천조건을 확인해 주세요. 출생연도 범위와 반경(5~500km)을 확인해 주세요.");
    }
}
