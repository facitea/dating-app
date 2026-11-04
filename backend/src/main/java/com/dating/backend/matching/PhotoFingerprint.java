package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor
@Table(indexes={@Index(columnList="photoId",unique=true),@Index(columnList="sha"),@Index(columnList="band0"),@Index(columnList="band1"),@Index(columnList="band2"),@Index(columnList="band3")})
public class PhotoFingerprint {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long photoId;private Long userId;@Column(length=500) private String url;
 @Column(length=64) private String sha;private long visual;private boolean informative;
 private int band0;private int band1;private int band2;private int band3;
}
