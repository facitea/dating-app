package com.dating.backend.matching;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="member_likes",uniqueConstraints=@UniqueConstraint(columnNames={"senderId","targetId"}))
@Getter @Setter @NoArgsConstructor
public class MemberLike {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long senderId;
    @Column(nullable=false) private Long targetId;
    private boolean active=true;
    private boolean dismissed=false;
    private Instant createdAt=Instant.now();
}
