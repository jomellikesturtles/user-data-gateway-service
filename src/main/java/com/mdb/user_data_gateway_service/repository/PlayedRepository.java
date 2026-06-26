package com.mdb.user_data_gateway_service.repository;

import com.mdb.user_data_gateway_service.entity.Played;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayedRepository extends JpaRepository<Played, Long> {
    List<Played> findByProfileId(String profileId, Pageable pageable);
    long countByProfileId(String profileId);
    Optional<Played> findByProfileIdAndMediaId(String profileId, String mediaId);
    void deleteByProfileIdAndMediaId(String profileId, String mediaId);
}
