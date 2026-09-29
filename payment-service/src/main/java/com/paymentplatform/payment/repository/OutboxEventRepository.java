package com.paymentplatform.payment.repository;

import com.paymentplatform.payment.domain.OutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = "select current_event.* from payment.outbox_events current_event where current_event.published_at is null and not exists (select 1 from payment.outbox_events earlier where earlier.aggregate_id = current_event.aggregate_id and earlier.published_at is null and (earlier.occurred_at < current_event.occurred_at or (earlier.occurred_at = current_event.occurred_at and earlier.id < current_event.id))) order by current_event.occurred_at, current_event.id limit :batchSize for update skip locked", nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("batchSize") int batchSize);
}
