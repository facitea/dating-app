package com.dating.backend.matching;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="credit_entries",uniqueConstraints=@UniqueConstraint(columnNames={"userId","referenceKey"}))
@Getter @Setter @NoArgsConstructor
public class CreditEntry {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long userId;
    private int amount;
    @Column(length=80) private String referenceKey;
    private Instant createdAt=Instant.now();
}
