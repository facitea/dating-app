package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="terms_consents") @Getter @Setter @NoArgsConstructor public class TermsConsent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long userId;private String version;private boolean terms;private boolean privacy;private boolean adult;private boolean marketing;private Instant acceptedAt=Instant.now();
}
