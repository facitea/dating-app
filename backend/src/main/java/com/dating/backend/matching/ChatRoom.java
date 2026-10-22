package com.dating.backend.matching;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="chat_rooms",uniqueConstraints=@UniqueConstraint(columnNames={"lowId","highId"}))
@Getter @Setter @NoArgsConstructor
public class ChatRoom {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long lowId;
    @Column(nullable=false) private Long highId;
    private boolean matched;
    private boolean ended;
    private long readLow;
    private long readHigh;
    private Long lastMessageId;
    @Column(length=2000) private String lastMessage;
    private Instant updatedAt=Instant.now();
}
