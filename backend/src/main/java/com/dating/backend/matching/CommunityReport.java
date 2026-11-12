package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="community_reports",uniqueConstraints=@UniqueConstraint(columnNames={"postId","reporterId"})) @Getter @Setter @NoArgsConstructor
public class CommunityReport {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long postId;private Long reporterId;@Column(length=500) private String reason;private Instant createdAt=Instant.now();
}
