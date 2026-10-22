package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="app_notices",uniqueConstraints=@UniqueConstraint(columnNames={"userId","eventKey"})) @Getter @Setter @NoArgsConstructor
public class AppNotice {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long userId;
 @Column(length=100) private String eventKey;
 private String title;
 @Column(length=2000) private String body;
 private String link;
 private Instant createdAt=Instant.now();
 private boolean seen;
 private int pushAttempts;
 private boolean pushed;
}
