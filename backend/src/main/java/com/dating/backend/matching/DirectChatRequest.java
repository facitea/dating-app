package com.dating.backend.matching;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** Missing row means an existing or mutually matched conversation, with no acceptance gate. */
@Entity @Table(name="direct_chat_requests", indexes=@Index(name="idx_direct_request_expiry",columnList="state,expiresAt"))
@Getter @Setter @NoArgsConstructor
public class DirectChatRequest {
    @Id private Long roomId;
    @Column(nullable=false) private Long senderId;
    @Column(nullable=false) private Long recipientId;
    private int cost;
    @Column(nullable=false,length=16) private String state="PENDING";
    @Column(nullable=false) private Instant expiresAt;
}
