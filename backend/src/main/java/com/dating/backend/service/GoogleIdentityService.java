package com.dating.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Service
public class GoogleIdentityService {
    @Value("${app.firebase.project-id:}") private String projectId;
    @Value("${app.firebase.credentials-file:}") private String credentialsFile;
    private FirebaseAuth auth;
    public record Identity(String uid, String email) {}
    private synchronized FirebaseAuth auth() {
        if(projectId.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"구글 로그인을 준비 중입니다. 이메일 로그인을 이용해 주세요.");
        if(auth==null) try {
            var options=FirebaseOptions.builder().setProjectId(projectId).setCredentials(credentials()).build();
            var app=FirebaseApp.getApps().stream().filter(a->a.getName().equals("dating-auth")).findFirst().orElseGet(()->FirebaseApp.initializeApp(options,"dating-auth"));
            auth=FirebaseAuth.getInstance(app);
        } catch(Exception e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"구글 로그인 서버 설정을 확인해 주세요."); }
        return auth;
    }
    private GoogleCredentials credentials() throws java.io.IOException {
        if (credentialsFile.isBlank()) return GoogleCredentials.getApplicationDefault();
        try (var stream = java.nio.file.Files.newInputStream(java.nio.file.Path.of(credentialsFile))) {
            return GoogleCredentials.fromStream(stream);
        }
    }
    public Identity verify(String token) {
        if(token==null||token.isBlank()||token.length()>16000) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"구글 인증을 다시 진행해 주세요.");
        var verifier=auth();
        try {
            var decoded=verifier.verifyIdToken(token,true);
            var firebase=decoded.getClaims().get("firebase");
            if(!(firebase instanceof Map<?,?> claims)||!"google.com".equals(claims.get("sign_in_provider"))||!decoded.isEmailVerified()||decoded.getEmail()==null)
                throw new IllegalArgumentException();
            var authTime=decoded.getClaims().get("auth_time");
            if(!(authTime instanceof Number n)||n.longValue()>java.time.Instant.now().getEpochSecond()+60||n.longValue()<java.time.Instant.now().minusSeconds(3600).getEpochSecond())throw new IllegalArgumentException();
            return new Identity(decoded.getUid(),decoded.getEmail());
        } catch(Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"구글 인증이 만료되었거나 유효하지 않습니다. 다시 로그인해 주세요."); }
    }
}
