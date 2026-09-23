package com.paymentplatform.ledger.repository;

import com.paymentplatform.ledger.domain.LedgerTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {
    Optional<LedgerTransaction> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
}
