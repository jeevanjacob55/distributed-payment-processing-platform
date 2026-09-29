package com.paymentplatform.ledger.event;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerEventProcessor {
    private final JdbcTemplate jdbcTemplate;
    private final LedgerPostingService postingService;

    public LedgerEventProcessor(JdbcTemplate jdbcTemplate, LedgerPostingService postingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.postingService = postingService;
    }

    @Transactional
    public void process(PaymentLifecycleEvent event) {
        int inserted = jdbcTemplate.update(
                "insert into ledger.consumed_events (event_id, event_type) values (?, ?) on conflict do nothing",
                event.eventId(),
                event.eventType());
        if (inserted == 0) {
            return;
        }
        if (!"payment.completed.v1".equals(event.eventType())
                && !"refund.completed.v1".equals(event.eventType())) {
            return;
        }

        postingService.post(event);
    }
}
