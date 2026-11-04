package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"leftId","rightId"}),indexes=@Index(columnList="reviewed,id"))
public class PhotoDuplicate {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long leftId;private Long rightId;private boolean exact;private int distance;
 private boolean reviewed;private Instant detectedAt=Instant.now();private Instant reviewedAt;
}
