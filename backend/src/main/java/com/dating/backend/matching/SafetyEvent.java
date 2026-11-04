package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="safety_events",indexes={@Index(name="idx_safety_created",columnList="createdAt"),@Index(name="idx_safety_user",columnList="userId,type")})
@Getter @Setter @NoArgsConstructor
public class SafetyEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Integer score; private Long userId; private Long targetId; private Long roomId;
 @Column(length=40,nullable=false) private String type;
 @Column(length=500) private String detail;
 private Instant createdAt=Instant.now();
 private boolean reviewed; @Column(length=1000) private String reviewNote;
}
