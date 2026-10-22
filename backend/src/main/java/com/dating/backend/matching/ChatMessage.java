package com.dating.backend.matching;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="chat_messages",uniqueConstraints=@UniqueConstraint(columnNames={"roomId","senderId","clientId"}), indexes=@Index(name="idx_chat_room_id",columnList="roomId,id"))
@Getter @Setter @NoArgsConstructor
public class ChatMessage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long roomId;
    @Column(nullable=false) private Long senderId;
    @Column(nullable=false,length=36) private String clientId;
    @Column(nullable=false,length=2000) private String body;
    private Instant createdAt=Instant.now();
}
