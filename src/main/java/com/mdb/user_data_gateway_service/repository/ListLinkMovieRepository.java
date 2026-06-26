package com.mdb.user_data_gateway_service.repository;

import com.mdb.user_data_gateway_service.entity.ListLinkMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListLinkMovieRepository extends JpaRepository<ListLinkMovie, String> {
    List<ListLinkMovie> findByListId(String listId);
    Optional<ListLinkMovie> findByListIdAndMediaId(String listId, String mediaId);
    boolean existsByListIdAndMediaId(String listId, String mediaId);
    void deleteByListIdAndMediaId(String listId, String mediaId);
    void deleteByListId(String listId);
}
