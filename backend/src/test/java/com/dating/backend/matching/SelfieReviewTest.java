package com.dating.backend.matching;
import com.dating.backend.entity.*;import com.dating.backend.service.FileStorageService;import jakarta.persistence.*;import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.transaction.annotation.Transactional;import org.springframework.mock.web.MockMultipartFile;import org.springframework.web.server.ResponseStatusException;import java.time.*;import java.util.*;import java.io.*;import javax.imageio.ImageIO;import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Transactional class SelfieReviewTest {
 @Autowired EntityManager em;@Autowired SelfieReviewService selfies;@Autowired FileStorageService storage;@Autowired PrivateMediaService media;
 User u,other;MockMultipartFile photo;
 @BeforeEach void setup()throws Exception{u=person();other=person();var image=new java.awt.image.BufferedImage(200,200,java.awt.image.BufferedImage.TYPE_INT_RGB);var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);photo=new MockMultipartFile("file","selfie.png","image/png",bytes.toByteArray());String url=storage.storeProfileImage(u.getId(),photo);em.persist(UserProfileImage.builder().userId(u.getId()).imageUrl(url).isMain(true).imageOrder(0).build());selfies.requestReview(u.getId(),"테스트 확인 요청");}
 User person(){var u=User.builder().email(UUID.randomUUID()+"@selfie.test").nickname(UUID.randomUUID().toString()).provider("LOCAL").status("ACTIVE").build();em.persist(u);return u;}
 @Test void consentOwnershipAndExpiredChallengesEnforced(){var request=selfies.start(u.getEmail());assertThrows(ResponseStatusException.class,()->selfies.submit(other.getEmail(),request.id(),true,photo));assertThrows(ResponseStatusException.class,()->selfies.submit(u.getEmail(),request.id(),false,photo));em.find(SelfieReview.class,request.id()).setChallengeExpires(Instant.now().minusSeconds(1));assertThrows(ResponseStatusException.class,()->selfies.submit(u.getEmail(),request.id(),true,photo));}
 @Test void uploadIsPrivateManualAndPurgedAfterRetention(){var started=selfies.start(u.getEmail());var submitted=selfies.submit(u.getEmail(),started.id(),true,photo);assertEquals("PENDING",submitted.state());var row=em.find(SelfieReview.class,started.id());assertNotNull(row.getConsentAt());assertNotNull(selfies.photo(started.id(),false));assertThrows(ResponseStatusException.class,()->media.path("selfie-review/"+row.getSelfieFile()));var decision=selfies.decide(started.id(),true,"대표 사진과 수동 비교 완료");assertEquals("APPROVED",decision.state());assertTrue(decision.deleteAt().isAfter(Instant.now().plus(Duration.ofDays(29))));row.setDeleteAt(Instant.now().minusSeconds(1));assertThrows(ResponseStatusException.class,()->selfies.photo(row.getId(),false));selfies.purge();assertNull(row.getSelfieFile());assertNotNull(row.getPurgedAt());}
 @Test void changedProfileRequiresNewReview(){var request=selfies.start(u.getEmail());selfies.submit(u.getEmail(),request.id(),true,photo);var profile=em.createQuery("select i from UserProfileImage i where i.userId=:u",UserProfileImage.class).setParameter("u",u.getId()).getSingleResult();profile.setImageUrl("/uploads/profile/changed.png");assertThrows(ResponseStatusException.class,()->selfies.decide(request.id(),true,"확인"));assertEquals("RETRY",selfies.decide(request.id(),false,"대표 사진 변경으로 재촬영 필요").state());}
 @Test void unauthorizedAdminRolesCannotSeeSelfiesOrReviewSignals(){assertFalse(com.dating.backend.admin.AdminFilter.allowed("READ_ONLY","GET","/api/admin/selfies/1/photo"));assertFalse(com.dating.backend.admin.AdminFilter.allowed("SUPPORT","GET","/api/admin/selfies/1/photo"));assertTrue(com.dating.backend.admin.AdminFilter.allowed("REVIEWER","GET","/api/admin/selfies/1/photo"));assertFalse(com.dating.backend.admin.AdminFilter.allowed("REVIEWER","PUT","/api/admin/safety/monitor/1"));assertTrue(com.dating.backend.admin.AdminFilter.allowed("SUPPORT","PUT","/api/admin/safety/monitor/1"));}

 @Autowired com.dating.backend.service.AdminReviewService reviews;
 @Autowired com.dating.backend.service.ProfileImageService profiles;
 @Test void signupCannotSubmitOrApproveWithoutConsentedAndReviewedSelfie(){
  u.setStatus("PENDING_REVIEW");
  em.persist(UserProfile.builder().userId(u.getId()).region("서울 강서구").job("개발자").mbti("INTJ").introduction("테스트 소개").smokingStatus("NON_SMOKER").drinkingStatus("NONE").religion("NONE").build());
  em.persist(UserVerification.builder().userId(u.getId()).birthDate(LocalDate.of(1995,1,1)).gender("MALE").isVerified(false).build());
  em.persist(UserProfileImage.builder().userId(u.getId()).imageUrl("/uploads/profile/second.jpg").isMain(false).imageOrder(1).build());
  assertThrows(ResponseStatusException.class,()->profiles.submitReview(u.getEmail()));
  assertThrows(ResponseStatusException.class,()->reviews.approve(u.getId()));
  var capture=selfies.start(u.getEmail());selfies.submit(u.getEmail(),capture.id(),true,photo);
  assertEquals("selfie-signup-required-2026-11-04",em.find(SelfieReview.class,capture.id()).getConsentVersion());
  profiles.submitReview(u.getEmail());
  assertThrows(ResponseStatusException.class,()->reviews.approve(u.getId()));
  selfies.decide(capture.id(),true,"프로필과 셀피 확인 완료");reviews.approve(u.getId());assertEquals("ACTIVE",u.getStatus());
 }
 @Test void withdrawalExpiryAndChangedPhotoInvalidateSignupEvidence(){
  var capture=selfies.start(u.getEmail());selfies.submit(u.getEmail(),capture.id(),true,photo);selfies.decide(capture.id(),true,"확인 완료");
  selfies.requireSignup(u.getId(),true);
  var row=em.find(SelfieReview.class,capture.id());row.setDeleteAt(Instant.now().minusSeconds(1));assertThrows(ResponseStatusException.class,()->selfies.requireSignup(u.getId(),true));
  row.setDeleteAt(Instant.now().plusSeconds(3600));row.setReferenceUrl("/uploads/profile/old.jpg");assertThrows(ResponseStatusException.class,()->selfies.requireSignup(u.getId(),true));
  selfies.withdrawConsent(u.getEmail());assertThrows(ResponseStatusException.class,()->selfies.requireSignup(u.getId(),false));
 }

 @Test void ordinaryActiveMemberCannotStartWithoutRequest(){selfies.cancelRequest(u.getId());assertThrows(ResponseStatusException.class,()->selfies.start(u.getEmail()));selfies.requestReview(u.getId(),"사진 확인 요청");assertTrue(selfies.requestStatus(u.getEmail()).required());assertThrows(ResponseStatusException.class,()->selfies.requestReview(u.getId(),"중복 요청"));}
 @Test void approvalNeedsNoCommentButRetryNeedsReason(){var c=selfies.start(u.getEmail());selfies.submit(u.getEmail(),c.id(),true,photo);assertThrows(ResponseStatusException.class,()->selfies.decide(c.id(),false,""));assertEquals("APPROVED",selfies.decide(c.id(),true,"").state());assertFalse(selfies.requestStatus(u.getEmail()).required());}
 @Test void cancelledRequestCannotSubmitAlreadyStartedCapture(){var c=selfies.start(u.getEmail());selfies.cancelRequest(u.getId());assertThrows(ResponseStatusException.class,()->selfies.submit(u.getEmail(),c.id(),true,photo));}
 @Autowired com.dating.backend.config.JwtUtil jwt;@Autowired org.springframework.web.context.WebApplicationContext context;
 @Test void requestedMemberBlockedAtApiUntilReviewOrCancellation()throws Exception{
  var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();String token=jwt.createToken(u.getEmail());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/social/notifications").header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value("SELFIE_REQUIRED"));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/selfie/request").header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
  selfies.cancelRequest(u.getId());assertFalse(jwt.selfieRequired(u.getEmail()));
 }
}
