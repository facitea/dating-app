package com.dating.backend.matching;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="community_posts",indexes={@Index(name="idx_community_parent_id",columnList="parentId,id"),@Index(name="idx_community_author_time",columnList="authorId,createdAt")}) @Getter @Setter @NoArgsConstructor
public class CommunityPost {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long authorId; private Long parentId;
 private String category; @Column(length=80) private String title;
 @Column(length=3000) private String body;
 private String imageKey;
 private boolean hidden;private boolean deleted;
 private Instant createdAt=Instant.now();
}
