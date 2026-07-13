package com.mdb.user_data_gateway_service.repository.interaction;

import com.mdb.user_data_gateway_service.entity.interaction.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMediaId(String mediaId, Pageable pageable);
    long countByMediaId(String mediaId);
    List<Review> findByProfileId(String profileId, Pageable pageable);
    long countByProfileId(String profileId);
}
