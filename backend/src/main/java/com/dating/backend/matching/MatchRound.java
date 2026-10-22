package com.dating.backend.matching;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="match_rounds", uniqueConstraints=@UniqueConstraint(columnNames={"ownerId","startsAt"}))
@Getter @Setter @NoArgsConstructor
public class MatchRound {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long ownerId;
    @Column(nullable=false) private Instant startsAt;
    private Long preferenceId;
}
