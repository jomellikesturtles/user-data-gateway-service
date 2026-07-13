package com.mdb.user_data_gateway_service.entity.interaction;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "review", indexes = {
    @Index(name = "idx_review_media", columnList = "media_id"),
    @Index(name = "idx_review_author", columnList = "profile_id")
})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "review_seq_gen")
    @SequenceGenerator(name = "review_seq_gen", sequenceName = "review_seq", allocationSize = 50)
    @Column(name = "id")
    private Long id;

    @Column(name = "content")
    private String content;

    @Column(name = "title")
    private String title;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "likes_num")
    private Integer likesNum;

    @Column(name = "dislikes_num")
    private Integer dislikesNum;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "profile_id")
    private String profileId;

    @Column(name = "media_id")
    private String mediaId;
}
