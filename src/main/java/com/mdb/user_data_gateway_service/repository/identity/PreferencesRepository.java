package com.mdb.user_data_gateway_service.repository.identity;

import com.mdb.user_data_gateway_service.entity.identity.Preferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreferencesRepository extends JpaRepository<Preferences, UUID> {
    Optional<Preferences> findByUserId(String userId);
    void deleteByUserId(String userId);
}
