package com.dating.backend.service;
import com.dating.backend.dto.*;
import com.dating.backend.entity.User;
import com.dating.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @Transactional
class GoogleLoginTest {
 @Autowired AuthService service;@Autowired UserRepository users;@Autowired PasswordEncoder passwords;@Autowired EntityManager em;
 @MockitoBean GoogleIdentityService identity;
 void verified(){when(identity.verify("valid")).thenReturn(new GoogleIdentityService.Identity("firebase-uid","google@example.test"));}
 SignupRequest request(){var r=new SignupRequest();r.setFirebaseIdToken("valid");r.setEmail("forged@example.test");r.setNickname("구글테스트");r.setBirthDate(LocalDate.of(1995,1,1));r.setGender("MALE");r.setRegion("서울 강남구");r.setJob("개발자");r.setMbti("INTJ");r.setPersonality("성격");r.setIdealType("이상형");r.setIntroduction("소개");r.setSmokingStatus("NON_SMOKER");r.setDrinkingStatus("NONE");r.setReligion("NONE");r.setAgreedToTerms(true);r.setAgreedToPrivacy(true);r.setAdultConfirmed(true);r.setTermsVersion("2026-10-29-test");return r;}
 @Test void unknownGoogleAccountDoesNotCreateUserUntilConsentedSignup(){verified();var result=service.googleLogin("valid",null);assertEquals("SIGNUP_REQUIRED",result.state());assertNull(result.auth());assertFalse(users.existsByEmail("google@example.test"));}
 @Test void googleSignupUsesVerifiedIdentityAndStillRequiresReview(){verified();var auth=service.signup(request());var u=users.findByEmail("google@example.test").orElseThrow();assertEquals("GOOGLE",u.getProvider());assertEquals("firebase-uid",u.getFirebaseUid());assertNull(u.getPassword());assertEquals("PENDING_REVIEW",u.getStatus());assertNotNull(auth.getToken());assertFalse(users.existsByEmail("forged@example.test"));assertEquals(1L,em.createQuery("select count(t) from TermsConsent t where t.userId=:id",Long.class).setParameter("id",u.getId()).getSingleResult());assertEquals("AUTHENTICATED",service.googleLogin("valid",null).state());}
 @Test void missingConsentAndUnderageCannotCreateGoogleAccount(){verified();var r=request();r.setAgreedToPrivacy(false);assertThrows(ResponseStatusException.class,()->service.signup(r));r.setAgreedToPrivacy(true);r.setBirthDate(LocalDate.now().minusYears(17));assertThrows(ResponseStatusException.class,()->service.signup(r));assertFalse(users.existsByEmail("google@example.test"));}
 @Test void existingEmailRequiresPasswordAndRetainsAccount(){verified();var u=users.save(User.builder().email("google@example.test").nickname("기존회원").provider("LOCAL").status("ACTIVE").password(passwords.encode("Existing123!")).build());assertEquals("LINK_REQUIRED",service.googleLogin("valid",null).state());assertNull(u.getFirebaseUid());assertThrows(ResponseStatusException.class,()->service.googleLogin("valid","Wrong123!"));assertNull(u.getFirebaseUid());var result=service.googleLogin("valid","Existing123!");assertEquals("AUTHENTICATED",result.state());assertEquals("firebase-uid",u.getFirebaseUid());assertEquals("기존회원",u.getNickname());assertTrue(passwords.matches("Existing123!",u.getPassword()));assertEquals("LOCAL",u.getProvider());}
 @Test void suspendedAndDeletedGoogleAccountsCannotSignIn(){verified();var u=users.save(User.builder().email("google@example.test").nickname("정지회원").provider("GOOGLE").firebaseUid("firebase-uid").status("SUSPENDED").build());assertThrows(ResponseStatusException.class,()->service.googleLogin("valid",null));u.setStatus("DELETED");assertThrows(ResponseStatusException.class,()->service.googleLogin("valid",null));}
 @Test void duplicateSignupCannotOverwriteExistingAccount(){verified();var u=users.save(User.builder().email("google@example.test").nickname("보존회원").provider("LOCAL").status("ACTIVE").password(passwords.encode("Existing123!")).build());assertThrows(ResponseStatusException.class,()->service.signup(request()));assertEquals("보존회원",u.getNickname());assertNull(u.getFirebaseUid());}
 @Test void unverifiedTokenCannotCreateSession(){when(identity.verify("invalid")).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));assertThrows(ResponseStatusException.class,()->service.googleLogin("invalid",null));}
}
