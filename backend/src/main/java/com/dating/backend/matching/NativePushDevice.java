package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="native_push_devices") @Getter @Setter @NoArgsConstructor public class NativePushDevice {
 @Id private String tokenHash;private Long userId;@Column(length=2048,nullable=false) private String token;private long sessionVersion;private Instant registeredAt=Instant.now();
}
