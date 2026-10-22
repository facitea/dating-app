package com.dating.backend.matching;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="notification_receipts",uniqueConstraints=@UniqueConstraint(columnNames={"userId","eventKey"}))
@Getter @Setter @NoArgsConstructor
public class NotificationReceipt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long userId;
    @Column(nullable=false,length=80) private String eventKey;
}
