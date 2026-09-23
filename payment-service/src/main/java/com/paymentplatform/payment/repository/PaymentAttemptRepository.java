package com.paymentplatform.payment.repository;

import com.paymentplatform.payment.domain.PaymentAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    List<PaymentAttempt> findByPayment_IdOrderByAttemptNumberAsc(UUID paymentId);
}
