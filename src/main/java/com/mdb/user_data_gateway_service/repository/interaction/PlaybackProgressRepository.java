package com.mdb.user_data_gateway_service.repository.interaction;

import com.mdb.user_data_gateway_service.entity.interaction.PlaybackProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PlaybackProgressRepository extends JpaRepository<PlaybackProgress, String> {
    Optional<PlaybackProgress> findByProfileIdAndMediaId(String profileId, String mediaId);
    java.util.List<PlaybackProgress> findByProfileId(String profileId, org.springframework.data.domain.Pageable pageable);
    long countByProfileId(String profileId);
    void deleteByProfileIdAndMediaId(String profileId, String mediaId);
}
