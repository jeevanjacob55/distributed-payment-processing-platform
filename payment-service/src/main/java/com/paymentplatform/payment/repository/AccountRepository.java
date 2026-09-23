package com.paymentplatform.payment.repository;

import com.paymentplatform.payment.domain.Account;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByUser_Id(UUID userId);
}
