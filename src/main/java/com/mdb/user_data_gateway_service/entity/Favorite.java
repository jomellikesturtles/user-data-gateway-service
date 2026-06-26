package com.mdb.user_data_gateway_service.entity;

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
@Table(name = "favorite", uniqueConstraints = { @UniqueConstraint(columnNames = { "profile_id", "tmdb_id" }) })
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "favorite_seq_gen")
    @SequenceGenerator(name = "favorite_seq_gen", sequenceName = "favorite_seq", allocationSize = 50)
    @Column(name = "id")
    private Long id;

    @Column(name = "profile_id")
    private String profileId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "title")
    private String title;

    @Column(name = "tmdb_id")
    private String tmdbId;

    @Column(name = "imdb_id")
    private String imdbId;

    @Column(name = "media_id", nullable = false)
    private String mediaId;

    @Column(name = "year")
    private Integer year;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
