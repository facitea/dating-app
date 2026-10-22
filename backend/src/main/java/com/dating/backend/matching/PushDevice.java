package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;
@Entity @Table(name="push_devices") @Getter @Setter @NoArgsConstructor
public class PushDevice {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long userId;
 @Column(length=64,unique=true) private String endpointHash;
 @Column(length=2000) private String endpoint;
 @Column(length=200) private String p256dh;
 @Column(length=100) private String auth;
}
