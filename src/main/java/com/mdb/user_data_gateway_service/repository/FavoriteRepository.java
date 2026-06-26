package com.mdb.user_data_gateway_service.repository;

import com.mdb.user_data_gateway_service.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndMediaId(String userId, String mediaId);
    boolean existsByUserIdAndMediaId(String userId, String mediaId);
    void deleteByUserIdAndMediaId(String userId, String mediaId);
    java.util.List<Favorite> findByUserId(String userId, org.springframework.data.domain.Pageable pageable);
    long countByUserId(String userId);
}
