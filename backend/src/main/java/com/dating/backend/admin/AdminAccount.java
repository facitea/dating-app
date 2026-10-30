package com.dating.backend.admin;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor public class AdminAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=80) private String username;
 @Column(nullable=false) private String passwordHash;
 @Column(nullable=false,length=20) private String role;
 private boolean enabled=true;
 private int failures;
 private Instant lockedUntil;
 private Instant createdAt=Instant.now();
}
