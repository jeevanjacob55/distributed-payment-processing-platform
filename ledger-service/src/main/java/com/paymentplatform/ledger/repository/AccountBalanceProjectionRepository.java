package com.paymentplatform.ledger.repository;

import com.paymentplatform.ledger.domain.AccountBalanceProjection;
import com.paymentplatform.ledger.domain.AccountBalanceProjectionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountBalanceProjectionRepository extends JpaRepository<AccountBalanceProjection, AccountBalanceProjectionId> {}
