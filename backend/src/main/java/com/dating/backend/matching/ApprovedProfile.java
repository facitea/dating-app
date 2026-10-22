package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;import java.time.*;import java.util.*;
@Entity @Table(name="approved_profiles") @Getter @Setter @NoArgsConstructor public class ApprovedProfile {
 @Id private Long userId;
 private String nickname;private String region;private String job;private String mbti;private String gender;private LocalDate birthDate;
 @Column(columnDefinition="TEXT") private String introduction;
 @Column(columnDefinition="TEXT") private String personality;
 @Column(columnDefinition="TEXT") private String idealType;
 private String smoking;private String drinking;private String religion;
 @ElementCollection @OrderColumn private List<String> images=new ArrayList<>();
}
