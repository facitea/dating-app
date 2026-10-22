package com.dating.backend.matching;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_match_cards", uniqueConstraints = @UniqueConstraint(
        name = "uk_daily_match_owner_day_slot", columnNames = {"owner_id", "cycle_day", "slot_number"}))
@Getter @Setter @NoArgsConstructor
public class DailyMatchCard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "owner_id", nullable = false) private Long ownerId;
    @Column(name = "cycle_day", nullable = false) private LocalDate cycleDay;
    @Column(name = "slot_number", nullable = false) private int slot;
    @Column(name = "candidate_id") private Long candidateId;
    @Column(length=36) private String purchaseRequestId;
    private Instant revealedAt;
    @Column(length = 10) private String reaction;
}
