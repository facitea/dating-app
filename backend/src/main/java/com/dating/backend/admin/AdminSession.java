package com.dating.backend.admin;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor public class AdminSession {
 @Id @Column(length=64) private String tokenHash;
 private Long adminId;
 @Column(length=64) private String csrf;
 private Instant expiresAt;
}
