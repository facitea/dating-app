/**
 * JwtUtil 설정
 */
package com.dating.backend.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final com.dating.backend.repository.UserRepository users;
    private final SecretKey secretKey;
    private final long expirationMillis;

    public JwtUtil(
            com.dating.backend.repository.UserRepository users,
            @Value("${app.jwt.secret:change-this-secret-key-change-this-secret-key}") String secret,
            @Value("${app.jwt.expiration-ms:3600000}") long expirationMillis
    ) {
        this.users=users;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String createToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("sv",users.findByEmail(email).map(com.dating.backend.entity.User::getSessionVersion).orElse(0L))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getEmail(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            var claims=Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
            var user=users.findByEmail(claims.getSubject()).orElse(null);if(user==null||"DELETED".equals(user.getStatus())||"SUSPENDED".equals(user.getStatus()))return false;
            Number version=claims.get("sv",Number.class);return user.getSessionVersion()==(version==null?0:version.longValue());
        } catch (Exception e) {
            return false;
        }
    }
}