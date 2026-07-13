package com.mdb.user_data_gateway_service.repository.identity;

import com.mdb.user_data_gateway_service.entity.identity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    User findByUserName(String userName);
    User findByEmail(String email);
    User findByEmailOrUserName(String email, String userName);
}
