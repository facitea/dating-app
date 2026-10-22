package com.dating.backend.matching;
import com.dating.backend.entity.*;
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
class SocialServiceTest {
 @Autowired InboxService inbox; @Autowired WalletService wallet; @Autowired com.dating.backend.service.ProfileImageService photos; @Autowired com.dating.backend.service.SafetyService safety;
 @Autowired NotificationService notifications; @Autowired EntityManager em; @Autowired SocialService social; @Autowired DailyMatchService daily; @Autowired MatchPreferenceService preferences;
 @MockitoBean DailyMatchSchedule schedule;
 User a,b,c; LocalDate day=LocalDate.of(2026,10,21);
 @BeforeEach void setup(){time("2026-10-21T10:00:00");a=person("a","MALE");b=person("b","FEMALE");c=person("c","MALE");preferences.save(a.getEmail(),new MatchPreferenceService.Conditions(null,null,"ANY","ANY","ANY","ANY","ANY"));known(a,b);var w=new CreditWallet();w.setUserId(a.getId());w.setBalance(100);em.persist(w);}
 void time(String s){when(schedule.current()).thenReturn(DailyMatchSchedule.at(LocalDateTime.parse(s).atZone(ZoneId.of("Asia/Seoul")).toInstant()));}
 User person(String name,String gender){var u=User.builder().email(name+"@social.test").nickname(name).provider("LOCAL").status("ACTIVE").build();em.persist(u);em.persist(UserProfile.builder().userId(u.getId()).region("서울 강남구").mbti("ESFJ").build());em.persist(UserVerification.builder().userId(u.getId()).gender(gender).birthDate(LocalDate.of(1995,1,1)).build());em.persist(UserProfileImage.builder().userId(u.getId()).imageUrl("/uploads/test.jpg").imageOrder(0).isMain(true).build());return u;}
 void known(User x,User y){var card=new DailyMatchCard();card.setOwnerId(x.getId());card.setCycleDay(day);card.setSlot(0);card.setCandidateId(y.getId());card.setRevealedAt(Instant.now());em.persist(card);}
 SocialService.DirectRequest request(){return new SocialService.DirectRequest(b.getId(),"안녕하세요",UUID.randomUUID().toString(),10);}
 @Test void paidRoomChargesOnceAndRetryDoesNotDuplicateMessage(){var r=request();var first=social.direct(a.getEmail(),r);var retry=social.direct(a.getEmail(),r);assertEquals(10,first.charged());assertEquals(0,retry.charged());assertEquals(first.roomId(),retry.roomId());assertEquals(90,retry.balance());assertEquals(1,social.messages(a.getEmail(),first.roomId(),0,0).messages().size());social.send(b.getEmail(),first.roomId(),new SocialService.SendRequest("반가워요",UUID.randomUUID().toString()));assertEquals(1,social.rooms(a.getEmail()).get(0).unread());assertEquals(2,social.messages(a.getEmail(),first.roomId(),0,0).messages().size());assertEquals(0,social.rooms(a.getEmail()).get(0).unread());}
 @Test void mutualLikesCreateOneFreeRoom(){assertFalse(social.like(a.getEmail(),b.getId()).matched());assertEquals(1,social.overview(b.getEmail()).received().size());var match=social.like(b.getEmail(),a.getId());assertTrue(match.matched());assertEquals(match.roomId(),social.like(a.getEmail(),b.getId()).roomId());assertEquals(1,social.overview(a.getEmail()).matched().size());assertEquals(0,social.quote(a.getEmail(),b.getId()).cost());assertEquals(0,social.direct(a.getEmail(),request()).charged());assertEquals(100,em.find(CreditWallet.class,a.getId()).getBalance());}
 @Test void insufficientCreditCreatesNothing(){em.find(CreditWallet.class,a.getId()).setBalance(9);assertThrows(ResponseStatusException.class,()->social.direct(a.getEmail(),request()));assertEquals(0,em.createQuery("select count(r) from ChatRoom r",Long.class).getSingleResult());assertEquals(9,em.find(CreditWallet.class,a.getId()).getBalance());}
 @Test void strangerCannotReadOrSendAndReportStopsContact(){var r=social.direct(a.getEmail(),request());assertEquals(404,assertThrows(ResponseStatusException.class,()->social.messages(c.getEmail(),r.roomId(),0,0)).getStatusCode().value());assertThrows(ResponseStatusException.class,()->social.send(c.getEmail(),r.roomId(),new SocialService.SendRequest("bad",UUID.randomUUID().toString())));em.persist(UserReport.builder().reporterUserId(b.getId()).reportedUserId(a.getId()).reasonCode("OTHER").detail("test").build());assertThrows(ResponseStatusException.class,()->social.send(a.getEmail(),r.roomId(),new SocialService.SendRequest("blocked",UUID.randomUUID().toString())));}
 @Test void unknownProfileCannotBePurchased(){assertThrows(ResponseStatusException.class,()->social.quote(c.getEmail(),b.getId()));}
 @Test void conditionsApplyAtNextRoundAndDoNotReroll(){
   // Existing known card is retained; add missing slots to mirror a complete deck.
   for(int i=1;i<6;i++){var card=new DailyMatchCard();card.setOwnerId(a.getId());card.setCycleDay(day);card.setSlot(i);em.persist(card);}
   for(int i=0;i<8;i++)person("pool"+i,"FEMALE");
   var before=daily.today(a.getEmail());
   var p=new MatchPreferenceService.Conditions(60,70,"ANY","ANY","ANY","ANY","ANY");
   var saved=preferences.save(a.getEmail(),p);assertNull(saved.active().ageMin());assertEquals(60,saved.pending().ageMin());assertEquals(before,daily.today(a.getEmail()));
   time("2026-10-21T20:00:00");var evening=daily.today(a.getEmail());assertEquals("OPEN",evening.cards().get(0).state());assertEquals("EMPTY",evening.cards().get(3).state());
   preferences.save(a.getEmail(),new MatchPreferenceService.Conditions(null,null,"ANY","ANY","ANY","ANY","ANY"));assertEquals(evening,daily.today(a.getEmail()));
 }
 @Test void allConditionsAndAnyAreHonored(){var mine=UserProfile.builder().region("서울 강남구").build();var other=UserProfile.builder().region("서울 강남구").mbti("ESFJ").build();var v=UserVerification.builder().birthDate(LocalDate.of(1995,1,1)).build();var p=new MatchPreference();p.setAgeMin(30);p.setAgeMax(35);p.setRegionScope("DISTRICT");p.setMbti("ESFJ");p.setSmoking("NON_SMOKER");p.setDrinking("NONE");p.setReligion("NONE");assertTrue(preferences.accepts(p,mine,other,v,day));other.setRegion("부산 해운대구");assertFalse(preferences.accepts(p,mine,other,v,day));p.setRegionScope("ANY");assertTrue(preferences.accepts(p,mine,other,v,day));other.setMbti("INTP");assertFalse(preferences.accepts(p,mine,other,v,day));p.setMbti("ANY");other.setSmokingStatus("SMOKER");assertFalse(preferences.accepts(p,mine,other,v,day));p.setSmoking("ANY");other.setDrinkingStatus("OFTEN");assertFalse(preferences.accepts(p,mine,other,v,day));p.setDrinking("ANY");other.setReligion("OTHER");assertFalse(preferences.accepts(p,mine,other,v,day));p.setReligion("ANY");assertTrue(preferences.accepts(p,mine,other,v,day));}

 @Test void allAnyCanBeSavedAndLoaded() {
   var any=new MatchPreferenceService.Conditions(null,null,"ANY","ANY","ANY","ANY","ANY",null,null,null);
   preferences.save(a.getEmail(),any);
   assertEquals(any,preferences.get(a.getEmail()).pending());
   assertEquals(2026,preferences.get(a.getEmail()).referenceYear());
 }
 @Test void birthYearAndRadiusRemainPendingUntilNextRound() {
   var selected=new MatchPreferenceService.Conditions(null,null,"ANY","ANY","ANY","ANY","ANY",1990,2000,30);
   var saved=preferences.save(a.getEmail(),selected);
   assertNull(saved.active().radiusKm()); assertEquals(30,saved.pending().radiusKm());
   time("2026-10-21T20:00:00");assertEquals(selected,preferences.get(a.getEmail()).active());
   var mine=UserProfile.builder().region("서울 강남구").build();
   var other=UserProfile.builder().region("서울 송파구").build();
   var v=UserVerification.builder().birthDate(LocalDate.of(1995,12,31)).build();
   var p=new MatchPreference();p.setBirthYearMin(1990);p.setBirthYearMax(2000);p.setRadiusKm(30);
   assertTrue(preferences.accepts(p,mine,other,v,day));
   v.setBirthDate(LocalDate.of(2001,1,1));assertFalse(preferences.accepts(p,mine,other,v,day));
   v.setBirthDate(LocalDate.of(1995,1,1));other.setRegion("부산 해운대구");assertFalse(preferences.accepts(p,mine,other,v,day));
   other.setRegion("미등록 지역");assertFalse(preferences.accepts(p,mine,other,v,day));
   p.setRadiusKm(null);assertTrue(preferences.accepts(p,mine,other,v,day));
 }
 @Test void invalidYearRangeAndRadiusAreRejected() {
   assertThrows(ResponseStatusException.class,()->preferences.save(a.getEmail(),new MatchPreferenceService.Conditions(null,null,"ANY","ANY","ANY","ANY","ANY",2000,1990,30)));
   assertThrows(ResponseStatusException.class,()->preferences.save(a.getEmail(),new MatchPreferenceService.Conditions(null,null,"ANY","ANY","ANY","ANY","ANY",1990,2000,501)));
 }

 @Test void recommendationBadgeReturnsAtEachNewRound() {
   assertTrue(notifications.get(a.getEmail()).today());
   notifications.acknowledge(a.getEmail(),new NotificationService.Seen(List.of(NotificationService.todayKey(schedule.current()))));
   assertFalse(notifications.get(a.getEmail()).today());
   assertTrue(notifications.get(b.getEmail()).today());
   time("2026-10-21T20:00:00");assertTrue(notifications.get(a.getEmail()).today());
   notifications.acknowledge(a.getEmail(),new NotificationService.Seen(List.of(NotificationService.todayKey(schedule.current()))));
   time("2026-10-22T07:59:59");assertFalse(notifications.get(a.getEmail()).today());
   time("2026-10-22T08:00:00");assertTrue(notifications.get(a.getEmail()).today());
 }
 @Test void receivedLikeAndNewMatchHaveSeparatePersistentBadges() {
   social.like(a.getEmail(),b.getId());assertEquals(1,notifications.get(b.getEmail()).matches());
   var key=social.overview(b.getEmail()).received().get(0).eventKey();
   notifications.acknowledge(b.getEmail(),new NotificationService.Seen(List.of(key)));
   em.flush();em.clear();assertEquals(0,notifications.get(b.getEmail()).matches());
   social.like(b.getEmail(),a.getId());assertEquals(1,notifications.get(b.getEmail()).matches());
   var matched=social.overview(b.getEmail()).matched().get(0).eventKey();
   notifications.acknowledge(a.getEmail(),new NotificationService.Seen(List.of(matched)));
   assertEquals(1,notifications.get(b.getEmail()).matches());
   notifications.acknowledge(b.getEmail(),new NotificationService.Seen(List.of(key)));
   assertEquals(1,notifications.get(b.getEmail()).matches());
   notifications.acknowledge(b.getEmail(),new NotificationService.Seen(List.of(matched,matched)));
   assertEquals(0,notifications.get(b.getEmail()).matches());
 }
 @Test void unreadChatBadgeClearsOnlyAfterRoomRead() {
   var room=social.direct(a.getEmail(),request()).roomId();
   assertEquals(1,notifications.get(b.getEmail()).chats());assertEquals(0,notifications.get(a.getEmail()).chats());
   social.rooms(b.getEmail());assertEquals(1,notifications.get(b.getEmail()).chats());
   social.messages(b.getEmail(),room,0,0);assertEquals(0,notifications.get(b.getEmail()).chats());
   social.send(a.getEmail(),room,new SocialService.SendRequest("새 소식",UUID.randomUUID().toString()));
   assertEquals(1,notifications.get(b.getEmail()).chats());
 }

 @Test void deletedChatIsPrivateAndNewMessagesRestoreOnlyNewHistory(){
  var room=social.direct(a.getEmail(),request()).roomId();social.deleteRoom(b.getEmail(),room);
  assertTrue(social.rooms(b.getEmail()).isEmpty());assertEquals(0,notifications.get(b.getEmail()).chats());
  assertEquals(1,social.messages(a.getEmail(),room,0,0).messages().size());
  social.send(a.getEmail(),room,new SocialService.SendRequest("new",UUID.randomUUID().toString()));
  assertEquals(1,social.rooms(b.getEmail()).size());var history=social.messages(b.getEmail(),room,0,0);
  assertEquals(1,history.messages().size());assertEquals("new",history.messages().get(0).text());assertFalse(history.hasOlder());
 }
 @Test void hidingMatchOnlyChangesOwnList(){
  social.like(a.getEmail(),b.getId());social.like(b.getEmail(),a.getId());social.hideMatch(a.getEmail(),b.getId());
  assertTrue(social.overview(a.getEmail()).matched().isEmpty());assertTrue(social.overview(a.getEmail()).sent().isEmpty());
  assertEquals(1,social.overview(b.getEmail()).matched().size());assertEquals(1,social.rooms(a.getEmail()).size());
  assertEquals(0,notifications.get(a.getEmail()).matches());
 }
 @Test void blockStopsBothDirectionsAndRemovesRecommendation(){
  var room=social.direct(a.getEmail(),request()).roomId();social.block(a.getEmail(),b.getId());social.block(a.getEmail(),b.getId());
  assertTrue(social.rooms(a.getEmail()).isEmpty());assertTrue(social.rooms(b.getEmail()).isEmpty());
  assertThrows(ResponseStatusException.class,()->social.send(b.getEmail(),room,new SocialService.SendRequest("bad",UUID.randomUUID().toString())));
  assertThrows(ResponseStatusException.class,()->social.profile(b.getEmail(),a.getId()));
  assertFalse(social.available(a.getId(),b.getId()));assertFalse(social.available(b.getId(),a.getId()));
  assertEquals("UNAVAILABLE",daily.today(a.getEmail()).cards().get(0).state());
 }
 @Test void profileRequiresKnownRevealedPersonAndDeletesRequireMembership(){
  assertEquals(b.getNickname(),social.profile(a.getEmail(),b.getId()).person().nickname());
  assertThrows(ResponseStatusException.class,()->social.profile(c.getEmail(),b.getId()));
  var room=social.direct(a.getEmail(),request()).roomId();
  assertThrows(ResponseStatusException.class,()->social.deleteRoom(c.getEmail(),room));
  assertThrows(ResponseStatusException.class,()->social.block(c.getEmail(),b.getId()));
 }

 @Test void peerReadReceiptAdvancesWithoutNewMessages(){
  var room=social.direct(a.getEmail(),request()).roomId();var initial=social.messages(a.getEmail(),room,0,0);var id=initial.messages().get(0).id();assertTrue(initial.peerReadThrough()<id);
  social.messages(b.getEmail(),room,0,0);var polled=social.messages(a.getEmail(),room,id,0);assertTrue(polled.messages().isEmpty());assertEquals(id,polled.peerReadThrough());
 }
 @Test void creditTopupIsIdempotentAndLedgerMatchesBalance(){
  var r=new WalletService.Topup(100,UUID.randomUUID().toString());assertEquals(200,wallet.topup(a.getEmail(),r).balance());assertEquals(200,wallet.topup(a.getEmail(),r).balance());assertEquals(1,wallet.get(a.getEmail()).entries().size());
  assertEquals(1,inbox.list(a.getEmail(),0).size());assertThrows(ResponseStatusException.class,()->wallet.topup(a.getEmail(),new WalletService.Topup(999,UUID.randomUUID().toString())));
 }
 @Test void inboxOwnershipAndReportCompletion(){
  social.direct(a.getEmail(),request());var note=inbox.list(b.getEmail(),0).get(0);assertEquals(1,inbox.unread(b.getEmail()));inbox.read(a.getEmail(),List.of(note.getId()));assertEquals(1,inbox.unread(b.getEmail()));inbox.read(b.getEmail(),List.of(note.getId()));assertEquals(0,inbox.unread(b.getEmail()));
  var report=UserReport.builder().reporterUserId(a.getId()).reportedUserId(b.getId()).reasonCode("OTHER").detail("test").build();em.persist(report);var req=new com.dating.backend.dto.ResolveReportRequest();req.setAdminNote("internal confidential note");safety.resolveReport(report.getId(),req);safety.resolveReport(report.getId(),req);
  var result=inbox.list(a.getEmail(),0);assertEquals(1,result.size());assertFalse(result.get(0).getBody().contains("confidential"));
 }
 @Test void onlyUploadedPhotoCountIsReturnedAndMainComesFirst(){
  em.persist(UserProfileImage.builder().userId(b.getId()).imageUrl("/uploads/second.jpg").imageOrder(1).isMain(false).build());var profile=social.profile(a.getEmail(),b.getId());assertEquals(2,profile.totalImages());assertEquals(1,profile.images().size());assertEquals("/uploads/test.jpg",profile.images().get(0));
  em.persist(UserProfileImage.builder().userId(a.getId()).imageUrl("/uploads/my-second.jpg").imageOrder(1).isMain(false).build());assertEquals(2,social.profile(a.getEmail(),b.getId()).images().size());
 }
 @Test void sixPhotoLimitOwnershipAndMainReplacement(){
  for(int i=1;i<=5;i++){var req=new com.dating.backend.dto.UserProfileImageRequest();req.setImageOrder(i);req.setImageUrl("/uploads/"+i+".jpg");req.setIsMain(false);photos.saveMyImage(a.getEmail(),req);}
  assertEquals(6,photos.getMyImages(a.getEmail()).size());var extra=new com.dating.backend.dto.UserProfileImageRequest();extra.setImageOrder(6);extra.setImageUrl("/uploads/6.jpg");extra.setIsMain(false);assertThrows(ResponseStatusException.class,()->photos.saveMyImage(a.getEmail(),extra));
  var id=em.createQuery("select i.id from UserProfileImage i where i.userId=:u and i.imageOrder=1",Long.class).setParameter("u",a.getId()).getSingleResult();photos.main(a.getEmail(),id);assertThrows(ResponseStatusException.class,()->photos.delete(b.getEmail(),id));photos.delete(a.getEmail(),id);
  assertEquals(1,em.createQuery("select count(i) from UserProfileImage i where i.userId=:u and i.isMain=true",Long.class).setParameter("u",a.getId()).getSingleResult());photos.saveMyImage(a.getEmail(),extra);assertEquals(6,photos.getMyImages(a.getEmail()).size());
 }
}
