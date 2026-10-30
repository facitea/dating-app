package com.dating.backend.admin;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor public class AdminAudit {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long adminId; private String username;
 @Column(length=300) private String action;
 private int status;
 private Instant createdAt=Instant.now();
}
