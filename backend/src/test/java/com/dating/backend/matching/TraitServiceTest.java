package com.dating.backend.matching;
import com.dating.backend.entity.*;
import com.dating.backend.dto.UpdateMyProfileRequest;
import com.dating.backend.service.ProfileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest @Transactional
class TraitServiceTest {
 @Autowired TraitService traits;@Autowired MatchPreferenceService preferences;@Autowired ProfileService profiles;
 @Autowired ProfileModeration moderation;@Autowired EntityManager em;@Autowired DailyMatchService daily;
 @MockitoBean DailyMatchSchedule schedule;
 User me,other;UserProfile mine,theirs;
 static final String OWN="{\"answers\":{\"personality\":[\"차분한\"]},\"preferences\":{}}";
 static final String OTHER="{\"answers\":{\"personality\":[\"활기찬\"]},\"preferences\":{}}";
 static final String WISH="{\"answers\":{},\"preferences\":{\"personality\":{\"mode\":\"SPECIFIC\",\"values\":[\"활기찬\"]}}}";
 @BeforeEach void setup(){time("2026-11-02T10:00:00");me=user("me","MALE");other=user("other","FEMALE");mine=profile(me);theirs=profile(other);mine.setTraitsJson(OWN);theirs.setTraitsJson(OTHER);}
 void time(String t){when(schedule.current()).thenReturn(DailyMatchSchedule.at(LocalDateTime.parse(t).atZone(ZoneId.of("Asia/Seoul")).toInstant()));}
 User user(String email,String gender){var u=User.builder().email(email+"@traits.test").nickname(email).status("ACTIVE").provider("LOCAL").build();em.persist(u);em.persist(UserProfile.builder().userId(u.getId()).region("서울 강남구").job("직장인").mbti("INTJ").personality("기존 성격").idealType("기존 이상형").introduction("소개").build());em.persist(UserVerification.builder().userId(u.getId()).gender(gender).birthDate(LocalDate.of(1995,1,1)).build());em.persist(UserProfileImage.builder().userId(u.getId()).imageUrl("/uploads/traits-test.jpg").imageOrder(0).isMain(true).build());return u;}
 UserProfile profile(User u){return em.createQuery("select p from UserProfile p where p.userId=:u",UserProfile.class).setParameter("u",u.getId()).getSingleResult();}
 MatchPreferenceService.Conditions any(String json){return new MatchPreferenceService.Conditions(null,null,"ANY","ANY","ANY","ANY","ANY",null,null,null,json);}
 @Test void knownKeywordsRoundTripAndDoNotInferWishes(){var normalized=traits.normalize(OWN);assertEquals(List.of("차분한"),traits.read(normalized).answers().get("personality"));assertTrue(traits.read(normalized).preferences().isEmpty());assertTrue(traits.read(traits.wishes(OWN)).answers().isEmpty());}
 @Test void rejectsUnknownDuplicateTooManyAndMalformedInput(){
  for(var json:List.of("null","[]","{\"extra\":true}","{\"answers\":{\"fake\":[\"차분한\"]}}",OWN.replace("차분한","가짜키워드"),OWN.replace("[\"차분한\"]","[\"차분한\",\"차분한\"]"),OWN.replace("[\"차분한\"]","[\"차분한\",\"밝은\",\"명랑한\",\"쾌활한\",\"발랄한\",\"활기찬\"]"),WISH.replace("SPECIFIC","UNKNOWN"),WISH.replace("[\"활기찬\"]","[]")))assertThrows(ResponseStatusException.class,()->traits.normalize(json),json);
 }
 @Test void specificPreferenceCanDifferFromMyOwnPersonality(){assertEquals(100,traits.assess(WISH,OWN,OTHER).percent());assertEquals(0,traits.assess(WISH,OWN,OWN).percent());assertEquals(1,traits.assess(WISH,OWN,OTHER).criteria());}
 @Test void similarUsesMyAnswerAndMissingOrAnyNeverBecomesCertain(){var similar="{\"preferences\":{\"personality\":{\"mode\":\"SIMILAR\",\"values\":[]}}}";assertEquals(100,traits.assess(similar,OWN,OWN).percent());assertEquals(0,traits.assess(similar,OWN,OTHER).percent());assertNull(traits.assess(similar,null,OTHER).percent());assertNull(traits.assess(WISH,OWN,null).percent());assertNull(traits.assess(null,OWN,OTHER).percent());}
 @Test void firstSetupUsesSignupWishesButLaterEditsWaitForNextRound(){mine.setTraitsJson("{\"answers\":{\"personality\":[\"차분한\"]},\"preferences\":{\"personality\":{\"mode\":\"SPECIFIC\",\"values\":[\"활기찬\"]}}}");assertFalse(preferences.get(me.getEmail()).configured());assertEquals(traits.wishes(WISH),preferences.get(me.getEmail()).active().traitPreferences());preferences.save(me.getEmail(),any(WISH));var first=preferences.active(me.getId(),schedule.current().morning());assertEquals(traits.wishes(WISH),first.getTraitPreferences());preferences.save(me.getEmail(),any(WISH.replace("활기찬","차분한")));assertEquals(traits.wishes(WISH),preferences.get(me.getEmail()).active().traitPreferences());time("2026-11-02T20:00:00");assertEquals(traits.wishes(WISH.replace("활기찬","차분한")),preferences.get(me.getEmail()).active().traitPreferences());}
 @Test void bothDirectionsAreIndependentAndCardHasTwoGauges(){preferences.save(me.getEmail(),any(WISH));preferences.save(other.getEmail(),any(WISH));var deck=daily.today(me.getEmail());assertNull(deck.cards().get(0).profile());var open=daily.reveal(me.getEmail(),deck.cycleDay(),0);assertEquals(100,open.profile().myFit().percent());assertEquals(0,open.profile().reciprocalFit().percent());}
 @Test void oldClientsDoNotEraseTraitPreferences(){preferences.save(me.getEmail(),any(WISH));preferences.save(me.getEmail(),any(null));assertEquals(traits.wishes(WISH),preferences.get(me.getEmail()).pending().traitPreferences());}
 UpdateMyProfileRequest update(){var q=new UpdateMyProfileRequest();q.setNickname(me.getNickname());q.setBirthDate(LocalDate.of(1995,1,1));q.setGender("MALE");q.setRegion(mine.getRegion());q.setJob(mine.getJob());q.setMbti(mine.getMbti());q.setPersonality(mine.getPersonality());q.setIdealType(mine.getIdealType());q.setIntroduction(mine.getIntroduction());q.setSmokingStatus(mine.getSmokingStatus());q.setDrinkingStatus(mine.getDrinkingStatus());q.setReligion(mine.getReligion());return q;}
 @Test void editingPersistsButApprovedTraitsRemainUntilReview(){preferences.save(me.getEmail(),any(null));var q=update();q.setTraitsJson(OTHER);var response=profiles.updateMyProfile(me.getEmail(),q);assertEquals(traits.normalize(OTHER),response.getTraitsJson());assertEquals("PENDING",me.getProfileReviewStatus());assertEquals(OWN,traits.approved(me.getId()));moderation.decide(me.getId(),me.getProfileRevision(),true,"승인");em.flush();assertEquals(traits.normalize(OTHER),traits.approved(me.getId()));}
 @Test void profileWishChangesScheduleNextRound(){preferences.save(me.getEmail(),any(null));var q=update();q.setTraitsJson(WISH);profiles.updateMyProfile(me.getEmail(),q);assertNull(preferences.get(me.getEmail()).active().traitPreferences());assertEquals(traits.wishes(WISH),preferences.get(me.getEmail()).pending().traitPreferences());}
}
