package com.mdb.user_data_gateway_service.repository;

import com.mdb.user_data_gateway_service.entity.Bookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserIdAndMediaId(String userId, String mediaId);
    List<Bookmark> findByUserId(String userId, Pageable pageable);
    boolean existsByUserIdAndMediaId(String userId, String mediaId);
    void deleteByUserIdAndMediaId(String userId, String mediaId);
    long countByUserId(String userId);
}
