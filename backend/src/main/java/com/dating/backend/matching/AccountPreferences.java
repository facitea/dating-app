package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;
@Entity @Table(name="account_preferences") @Getter @Setter @NoArgsConstructor public class AccountPreferences {
 @Id private Long userId;
 private boolean likes=true;private boolean matches=true;private boolean messages=true;private boolean recommendations=true;private boolean service=true;
 private boolean quietHours;private int quietStart=22;private int quietEnd=8;
}
