package com.mdb.user_data_gateway_service.repository.interaction;

import com.mdb.user_data_gateway_service.entity.interaction.MediaList;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaListRepository extends JpaRepository<MediaList, String> {
    List<MediaList> findByUserId(String userId, Pageable pageable);
    long countByUserId(String userId);
    Optional<MediaList> findByUserIdAndName(String userId, String name);
    boolean existsByUserIdAndName(String userId, String name);
}
