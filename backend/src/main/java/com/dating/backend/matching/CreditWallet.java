package com.dating.backend.matching;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="credit_wallets") @Getter @Setter @NoArgsConstructor
public class CreditWallet {
    @Id private Long userId;
    @Column(nullable=false) private int balance;
}
