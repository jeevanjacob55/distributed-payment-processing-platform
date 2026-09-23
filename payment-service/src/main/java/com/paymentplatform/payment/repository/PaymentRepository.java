package com.paymentplatform.payment.repository;

import com.paymentplatform.payment.domain.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPayerAccount_IdAndMerchantReference(UUID payerAccountId, String merchantReference);
}
