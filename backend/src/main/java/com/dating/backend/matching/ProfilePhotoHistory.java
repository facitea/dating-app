package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="profile_photo_history",indexes={@Index(name="idx_photo_history_user",columnList="userId,archivedAt"),@Index(name="idx_photo_history_expiry",columnList="expiresAt")}) @Getter @Setter @NoArgsConstructor
public class ProfilePhotoHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long userId;@Column(length=500) private String sourceUrl;private String filename;private String reason;
 private Instant archivedAt=Instant.now();private Instant expiresAt;private Instant purgedAt;private long bytes;private boolean sourceCleaned;
}
