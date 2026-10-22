package com.dating.backend.matching;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="match_preferences", indexes=@Index(name="idx_preference_owner_effective", columnList="ownerId,effectiveAt"))
@Getter @Setter @NoArgsConstructor
public class MatchPreference {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long ownerId;
    @Column(nullable=false) private Instant effectiveAt;
    private Integer birthYearMin;
    private Integer birthYearMax;
    private Integer radiusKm;
    private Integer ageMin;
    private Integer ageMax;
    private String regionScope = "ANY";
    private String mbti = "ANY";
    private String smoking = "ANY";
    private String drinking = "ANY";
    private String religion = "ANY";
}
