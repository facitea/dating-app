package com.dating.backend.matching;

import com.dating.backend.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class DailyMatchServiceTest {
    @Autowired DailyMatchService service;
    @Autowired MatchPreferenceService preferences;
    @Autowired EntityManager em;
    @MockitoBean DailyMatchSchedule schedule;
    User owner;
    final LocalDate day = LocalDate.of(2026, 10, 21);

    @BeforeEach void setup() {
        time("2026-10-21T08:00:00");
        owner = person("owner", "MALE", "ACTIVE");
        preferences.save(owner.getEmail(),new MatchPreferenceService.Conditions(null,null,"ANY","ANY","ANY","ANY","ANY"));
    }
    void time(String local) {
        when(schedule.current()).thenReturn(DailyMatchSchedule.at(LocalDateTime.parse(local)
                .atZone(ZoneId.of("Asia/Seoul")).toInstant()));
    }
    User person(String name, String gender, String status) {
        var u = User.builder().email(name + "@example.test").nickname(name).provider("LOCAL").status(status).build();
        em.persist(u);
        em.persist(UserProfile.builder().userId(u.getId()).region("서울 강남구").introduction("안녕하세요").build());
        em.persist(UserVerification.builder().userId(u.getId()).gender(gender).birthDate(LocalDate.of(1995, 1, 1)).build());
        em.persist(UserProfileImage.builder().userId(u.getId()).imageOrder(0).isMain(true).imageUrl("/uploads/profile/test.jpg").build());
        return u;
    }
    List<DailyMatchCard> saved(LocalDate date) {
        return em.createQuery("select c from DailyMatchCard c where c.ownerId = :owner and c.cycleDay = :day order by c.slot", DailyMatchCard.class)
                .setParameter("owner", owner.getId()).setParameter("day", date).getResultList();
    }
    void sixCandidates() { for (int i = 0; i < 6; i++) person("candidate" + i, "FEMALE", "ACTIVE"); }

    @Test void hiddenAndLockedCardsDoNotLeakProfilesAndRefreshKeepsSameDeck() {
        sixCandidates();
        var deck = service.today(owner.getEmail());
        assertEquals(6, deck.cards().size());
        assertEquals(List.of("HIDDEN", "HIDDEN", "HIDDEN", "LOCKED", "LOCKED", "LOCKED"), deck.cards().stream().map(DailyMatchService.Card::state).toList());
        assertTrue(deck.cards().stream().allMatch(c -> c.profile() == null));
        var ids = saved(day).stream().map(DailyMatchCard::getCandidateId).toList();
        service.today(owner.getEmail());
        assertEquals(ids, saved(day).stream().map(DailyMatchCard::getCandidateId).toList());
        assertEquals(3, ids.stream().filter(Objects::nonNull).distinct().count());
    }
    @Test void onlyOppositeActiveUnreportedAdultsAreSelected() {
        var eligible = person("eligible", "FEMALE", "ACTIVE");
        person("sameGender", "MALE", "ACTIVE");
        person("suspended", "FEMALE", "SUSPENDED");
        var minor = person("minor", "FEMALE", "ACTIVE");
        em.createQuery("select v from UserVerification v where v.userId = :id", UserVerification.class)
                .setParameter("id", minor.getId()).getSingleResult().setBirthDate(day.minusYears(17));
        var reported = person("reported", "FEMALE", "ACTIVE");
        em.persist(UserReport.builder().reporterUserId(owner.getId()).reportedUserId(reported.getId()).reasonCode("OTHER").detail("test").build());
        var deck = service.today(owner.getEmail());
        assertEquals(List.of(eligible.getId()), saved(day).stream().map(DailyMatchCard::getCandidateId).filter(Objects::nonNull).toList());
        assertEquals("EMPTY", deck.cards().get(1).state());
    }
    @Test void lockedRevealIsRejectedAndEveningPreservesMorning() {
        sixCandidates(); service.today(owner.getEmail());
        var error = assertThrows(ResponseStatusException.class, () -> service.reveal(owner.getEmail(), day, 3));
        assertEquals(403, error.getStatusCode().value());
        assertNotNull(service.reveal(owner.getEmail(), day, 0).profile());
        assertEquals("LIKE", service.react(owner.getEmail(), day, 0, "LIKE").reaction());
        time("2026-10-21T20:00:00");
        var deck = service.today(owner.getEmail());
        assertEquals("OPEN", deck.cards().get(0).state());
        assertEquals("LIKE", deck.cards().get(0).reaction());
        assertEquals("HIDDEN", deck.cards().get(3).state());
        assertNotNull(service.reveal(owner.getEmail(), day, 3).profile());
    }
    @Test void nextMorningReplacesDeckAndRejectsYesterdayActions() {
        for (int i = 0; i < 14; i++) person("candidate" + i, "FEMALE", "ACTIVE");
        service.today(owner.getEmail()); service.reveal(owner.getEmail(), day, 0);
        var yesterday = saved(day).stream().map(DailyMatchCard::getCandidateId).toList();
        time("2026-10-22T07:59:59");
        assertEquals(day, service.today(owner.getEmail()).cycleDay());
        time("2026-10-22T08:00:00");
        var deck = service.today(owner.getEmail());
        assertEquals(day.plusDays(1), deck.cycleDay());
        assertTrue(saved(day.plusDays(1)).stream().filter(c -> c.getCandidateId()!=null).noneMatch(c -> yesterday.contains(c.getCandidateId())));
        assertTrue(deck.cards().stream().allMatch(c -> c.profile() == null && c.reaction() == null));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.reveal(owner.getEmail(), day, 0)).getStatusCode().value());
    }
    @Test void suspendedCandidateDisappearsAndOtherUsersCannotOpenOwnersDeck() {
        var other = person("other", "FEMALE", "ACTIVE");
        preferences.save(other.getEmail(),new MatchPreferenceService.Conditions(null,null,"ANY","ANY","ANY","ANY","ANY"));
        service.today(owner.getEmail()); service.reveal(owner.getEmail(), day, 0);
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.reveal(other.getEmail(), day, 0)).getStatusCode().value());
        other.setStatus("SUSPENDED");
        var deck = service.today(owner.getEmail());
        assertTrue(deck.cards().stream().noneMatch(c->c.slot()==0));
        assertTrue(deck.cards().stream().noneMatch(c->"UNAVAILABLE".equals(c.state())));
    }

    @Test void recommendationsNeverRepeatEvenAfterSeveralDaysAndExcludeMatches() {
        var old=person("historical","FEMALE","ACTIVE");
        var historical=new DailyMatchCard();historical.setOwnerId(owner.getId());historical.setCycleDay(day.minusDays(40));historical.setSlot(0);historical.setCandidateId(old.getId());em.persist(historical);
        var matched=person("matched","FEMALE","ACTIVE");var room=new ChatRoom();room.setLowId(Math.min(owner.getId(),matched.getId()));room.setHighId(Math.max(owner.getId(),matched.getId()));room.setMatched(true);em.persist(room);
        var fresh=person("fresh","FEMALE","ACTIVE");service.today(owner.getEmail());
        assertEquals(List.of(fresh.getId()),saved(day).stream().map(DailyMatchCard::getCandidateId).filter(Objects::nonNull).toList());
        time("2026-10-25T08:00:00");assertTrue(service.today(owner.getEmail()).cards().stream().filter(c->c.slot()<3).allMatch(c->c.state().equals("EMPTY")));
    }
    @Test void reciprocalFitCountsOnlySpecifiedCriteriaAndAnyIsNotOneHundredPercent() {
        var mine=UserProfile.builder().region("서울 강남구").mbti("INTJ").smokingStatus("SMOKER").build();
        var theirs=UserProfile.builder().region("서울 강남구").build();var v=UserVerification.builder().birthDate(LocalDate.of(1995,1,1)).build();
        assertNull(preferences.fit(null,theirs,mine,v,day).percent());
        var pref=new MatchPreference();assertNull(preferences.fit(pref,theirs,mine,v,day).percent());
        pref.setMbti("INTJ");pref.setSmoking("NON_SMOKER");
        var fit=preferences.fit(pref,theirs,mine,v,day);assertEquals(50,fit.percent());assertEquals(2,fit.criteria());
        mine.setSmokingStatus("NON_SMOKER");assertEquals(100,preferences.fit(pref,theirs,mine,v,day).percent());
    }
    @Test void reciprocalFitRanksCompatibleCandidatesWithoutUsingPendingConditions() {
        var compatible=person("compatible","FEMALE","ACTIVE");
        var distant=em.createQuery("select p from UserProfile p where p.userId=:u",UserProfile.class).setParameter("u",compatible.getId()).getSingleResult();distant.setRegion("부산 해운대구");
        preferences.save(compatible.getEmail(),new MatchPreferenceService.Conditions(25,35,"ANY","ANY","ANY","ANY","ANY"));
        preferences.save(compatible.getEmail(),new MatchPreferenceService.Conditions(60,70,"ANY","ANY","ANY","ANY","ANY"));
        for(int i=0;i<5;i++)person("near"+i,"FEMALE","ACTIVE");
        service.today(owner.getEmail());var card=service.reveal(owner.getEmail(),day,0);
        assertEquals(compatible.getId(),card.profile().userId());assertEquals(100,card.profile().reciprocalFit().percent());
        assertEquals(1,card.profile().reciprocalFit().criteria());
    }
    @Test void matchedCardRemainsButRestrictedCardsDisappearWithoutChangingOtherSlots() {
        sixCandidates();service.today(owner.getEmail());
        var first=service.reveal(owner.getEmail(),day,0);var second=service.reveal(owner.getEmail(),day,1);
        var room=new ChatRoom();room.setLowId(Math.min(owner.getId(),first.profile().userId()));room.setHighId(Math.max(owner.getId(),first.profile().userId()));room.setMatched(true);em.persist(room);
        var matched=service.today(owner.getEmail()).cards().stream().filter(c->c.slot()==0).findFirst().orElseThrow();
        assertEquals("MATCHED",matched.state());assertEquals(first.profile().userId(),matched.profile().userId());
        var blocked=new MemberVisibility();blocked.setOwnerId(first.profile().userId());blocked.setTargetId(owner.getId());blocked.setBlocked(true);em.persist(blocked);
        var deck=service.today(owner.getEmail());assertTrue(deck.cards().stream().noneMatch(c->c.slot()==0));
        assertEquals(second,deck.cards().stream().filter(c->c.slot()==1).findFirst().orElseThrow());
        assertEquals(second,service.reveal(owner.getEmail(),day,1));
    }
    void removeSetup() {
        em.createQuery("delete from MatchPreference p where p.ownerId=:id").setParameter("id",owner.getId()).executeUpdate();
    }
    @Test void noRecommendationsOrPurchasesBeforeFirstSetup() {
        removeSetup(); sixCandidates();
        assertFalse(preferences.get(owner.getEmail()).configured());
        assertEquals(428,assertThrows(ResponseStatusException.class,()->service.today(owner.getEmail())).getStatusCode().value());
        assertEquals(428,assertThrows(ResponseStatusException.class,()->service.extraQuote(owner.getEmail())).getStatusCode().value());
        assertEquals(428,assertThrows(ResponseStatusException.class,()->service.buyExtra(owner.getEmail(),new DailyMatchService.ExtraPurchase(day,10,UUID.randomUUID().toString()))).getStatusCode().value());
        assertTrue(saved(day).isEmpty());
        assertEquals(0L,em.createQuery("select count(r) from MatchRound r where r.ownerId=:id",Long.class).setParameter("id",owner.getId()).getSingleResult());
    }
    @Test void firstConditionsApplyImmediatelyAndFurtherChangesWait() {
        removeSetup(); sixCandidates(); time("2026-10-21T21:00:00");
        var conditions=new MatchPreferenceService.Conditions(null,null,"ANY","INTJ","ANY","ANY","ANY");
        var first=preferences.save(owner.getEmail(),conditions);
        assertTrue(first.configured());assertNull(first.pending());assertEquals("INTJ",first.active().mbti());
        var deck=service.today(owner.getEmail());
        assertTrue(deck.cards().stream().allMatch(c->c.state().equals("EMPTY")));
        var changed=preferences.save(owner.getEmail(),new MatchPreferenceService.Conditions(null,null,"ANY","ANY","ANY","ANY","ANY"));
        assertEquals("INTJ",changed.active().mbti());assertEquals("ANY",changed.pending().mbti());
        assertEquals(deck,service.today(owner.getEmail()));
        time("2026-10-22T08:00:00");
        assertEquals("HIDDEN",service.today(owner.getEmail()).cards().get(0).state());
    }
    @Test void firstSetupPreservesOpenedLegacyCardsButRefiltersUnopenedOnes() {
        sixCandidates();service.today(owner.getEmail());var opened=service.reveal(owner.getEmail(),day,0);
        removeSetup();
        preferences.save(owner.getEmail(),new MatchPreferenceService.Conditions(null,null,"ANY","INTJ","ANY","ANY","ANY"));
        var deck=service.today(owner.getEmail());
        assertEquals(opened.slot(),deck.cards().get(0).slot()); assertEquals(opened.state(),deck.cards().get(0).state()); assertEquals(opened.profile().userId(),deck.cards().get(0).profile().userId()); assertEquals(0,deck.cards().get(0).profile().myFit().percent());
        assertEquals("EMPTY",deck.cards().get(1).state());assertEquals("EMPTY",deck.cards().get(2).state());
    }
    @Test void explicitAnyIsACompletedSetupIncludingBeforeEight() {
        removeSetup();time("2026-10-22T07:00:00");sixCandidates();
        var first=preferences.save(owner.getEmail(),new MatchPreferenceService.Conditions(null,null,"ANY","ANY","ANY","ANY","ANY"));
        assertTrue(first.configured());assertNull(first.pending());
        assertEquals(day,service.today(owner.getEmail()).cycleDay());
        assertEquals("HIDDEN",service.today(owner.getEmail()).cards().get(0).state());
    }
}
