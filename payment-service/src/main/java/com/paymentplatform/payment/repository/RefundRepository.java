package com.paymentplatform.payment.repository;

import com.paymentplatform.payment.domain.Refund;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    List<Refund> findByPayment_Id(UUID paymentId);
    boolean existsByReference(String reference);

    @Query(
            "select coalesce(sum(r.amount), 0) from Refund r "
                    + "where r.payment.id = :paymentId "
                    + "and r.status = com.paymentplatform.payment.domain.RefundStatus.COMPLETED")
    java.math.BigDecimal totalCompletedAmountByPaymentId(@Param("paymentId") UUID paymentId);
}
