package com.mdb.user_data_gateway_service.repository.interaction;

import com.mdb.user_data_gateway_service.entity.interaction.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByEmail(String email);
    Optional<AdminUser> findByUsername(String username);
    Optional<AdminUser> findByUsernameOrEmail(String username, String email);
}
