package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="selfie_reviews",indexes=@Index(name="idx_selfie_user",columnList="userId,createdAt")) @Getter @Setter @NoArgsConstructor
public class SelfieReview {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long userId; private String state="CAPTURE";private String challenge;private Instant challengeExpires;
 private String selfieFile;private String referenceFile;private String referenceUrl;
 private Instant createdAt=Instant.now();private Instant submittedAt;private Instant consentAt;private String consentVersion;
 private Instant reviewedAt;private Instant deleteAt;private Instant purgedAt;
 @Column(length=1000) private String note;
}
