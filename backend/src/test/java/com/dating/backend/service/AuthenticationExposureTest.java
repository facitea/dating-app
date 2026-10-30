package com.dating.backend.service;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.web.server.ResponseStatusException;import com.dating.backend.dto.*;import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest class AuthenticationExposureTest {
 @Autowired AuthService auth;@Autowired AuthVerificationService verification;
 @Test void resetTokenAndEmailCodeAreNotPubliclyReturned(){var reset=new ForgotPasswordRequest();reset.setEmail("victim@example.com");assertEquals(503,assertThrows(ResponseStatusException.class,()->auth.requestPasswordReset(reset)).getStatusCode().value());var request=new AuthVerificationRequest();request.setTargetType("EMAIL");request.setTargetValue("victim@example.com");request.setPurpose("SIGNUP");assertEquals(503,assertThrows(ResponseStatusException.class,()->verification.requestCode(request)).getStatusCode().value());}
 @Autowired PhoneAuthService phone;
 @Test void removedPhoneLoginCannotBypassSignupConsent(){assertEquals(410,assertThrows(ResponseStatusException.class,()->phone.requestCode(new PhoneVerificationRequest())).getStatusCode().value());assertEquals(410,assertThrows(ResponseStatusException.class,()->phone.verifyAndLogin(new PhoneVerificationConfirmRequest())).getStatusCode().value());}
}
