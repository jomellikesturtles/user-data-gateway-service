package com.mdb.user_data_gateway_service.repository.identity;

import com.mdb.user_data_gateway_service.entity.identity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Account findByEmail(String email);
}
