package com.dating.backend.matching;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="member_visibility",uniqueConstraints=@UniqueConstraint(columnNames={"ownerId","targetId"}))
@Getter @Setter @NoArgsConstructor
public class MemberVisibility {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long ownerId;
 @Column(nullable=false) private Long targetId;
 private boolean blocked;
 private boolean matchHidden;
 private boolean chatHidden;
 private long chatHiddenThrough;
}
