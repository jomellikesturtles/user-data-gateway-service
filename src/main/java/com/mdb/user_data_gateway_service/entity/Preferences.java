package com.mdb.user_data_gateway_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "preferences")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Preferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    @Builder.Default
    private String language = "en";

    @Column(name = "is_dark_mode", nullable = false)
    @Builder.Default
    private Boolean isDarkMode = false;

    @Column(name = "is_auto_scan_enabled")
    @Builder.Default
    private Boolean isAutoScanEnabled = false;

    @Column(name = "torrent_seed_ratio")
    private Double torrentSeedRatio;

    @Column(name = "auto_play_trailer", nullable = false)
    @Builder.Default
    private Boolean autoPlayTrailer = true;

    @Column(name = "play_trailer_before_show", nullable = false)
    @Builder.Default
    private Boolean playTrailerBeforeShow = false;

    @Column(name = "is_favorites_private", nullable = false)
    @Builder.Default
    private Boolean isFavoritesPrivate = false;

    @Column(name = "is_bookmarks_private", nullable = false)
    @Builder.Default
    private Boolean isBookmarksPrivate = false;

    @Column(name = "is_played_private", nullable = false)
    @Builder.Default
    private Boolean isPlayedPrivate = false;

    @Column(name = "is_progress_private", nullable = false)
    @Builder.Default
    private Boolean isProgressPrivate = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
