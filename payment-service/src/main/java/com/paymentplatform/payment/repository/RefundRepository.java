package com.paymentplatform.payment.repository;

import com.paymentplatform.payment.domain.Refund;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    List<Refund> findByPayment_Id(UUID paymentId);
}
