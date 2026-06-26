package com.mdb.user_data_gateway_service.repository;

import com.mdb.user_data_gateway_service.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, String> {
    List<Profile> findByUserId(String userId);
    Optional<Profile> findByUserIdAndName(String userId, String name);
    Optional<Profile> findByUserIdAndIsMain(String userId, Boolean isMain);
    void deleteByUserIdAndName(String userId, String name);
}
