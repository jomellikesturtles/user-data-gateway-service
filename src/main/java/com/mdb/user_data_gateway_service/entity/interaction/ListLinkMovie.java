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
@Table(name = "list_link_movie", uniqueConstraints = {
    @UniqueConstraint(name = "uk_list_media", columnNames = {"list_id", "media_id"}),
    @UniqueConstraint(columnNames = {"user_id", "list_id", "tmdb_id"}),
    @UniqueConstraint(columnNames = {"user_id", "list_id", "media_id"})
})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListLinkMovie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "list_id")
    private String listId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "profile_id")
    private String profileId;

    @Column(name = "media_id", nullable = false)
    private String mediaId;

    @Column(name = "tmdb_id")
    private String tmdbId;

    @Column(name = "imdb_id")
    private String imdbId;

    @Column(name = "title")
    private String title;

    @Column(name = "year")
    private Integer year;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
